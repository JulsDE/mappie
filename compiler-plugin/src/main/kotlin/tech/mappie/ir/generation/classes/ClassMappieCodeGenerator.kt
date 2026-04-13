package tech.mappie.ir.generation.classes

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.getKFunctionType
import tech.mappie.ir.MappieContext
import tech.mappie.exceptions.MappiePanicException.Companion.panic
import tech.mappie.ir.generation.ClassMappieCodeGenerationModel
import tech.mappie.ir.generation.MappieCodeGenerator
import tech.mappie.ir.generation.constructTransformation
import tech.mappie.ir.reporting.pretty
import tech.mappie.ir.referenceFunctionLet
import tech.mappie.ir.referenceFunctionRequireNotNull
import tech.mappie.ir.resolving.classes.sources.*
import tech.mappie.ir.resolving.classes.targets.ClassMappingTarget
import tech.mappie.ir.resolving.classes.targets.FunctionCallTarget
import tech.mappie.ir.resolving.classes.targets.SetterTarget
import tech.mappie.ir.resolving.classes.targets.ValueParameterTarget
import tech.mappie.ir.util.irLambda
import tech.mappie.ir.util.isMappieUpdateFromFunction
import tech.mappie.ir.referenceFunctionRun
import tech.mappie.ir.resolving.TargetSourcesClassMappings

class ClassMappieCodeGenerator(
    override val model: ClassMappieCodeGenerationModel
) : MappieCodeGenerator(model) {

    context(context: MappieContext)
    override fun IrBlockBodyBuilder.content() {
        val mapFunction = model.definition.referenceMapFunction()
        val constructor = model.constructor.symbol
        val regularParameters = mapFunction.parameters.filter { it.kind == IrParameterKind.Regular }
        val typeArguments = (mapFunction.returnType.type as IrSimpleType).arguments.map { it.typeOrNull ?: context.pluginContext.irBuiltIns.anyType }
        val mappings  = model.mappings as TargetSourcesClassMappings
        val updateInPlace = mapFunction.isMappieUpdateFromFunction() && mappings
            .filterKeys { it is ValueParameterTarget }
            .all { (_, source) -> source.single().preservesUpdaterValue() }

        if (updateInPlace) {
            val updater = regularParameters.last()
            applySetterMappings(mappings, regularParameters, irGet(updater), skipUpdaterFallbackMappings = true)
            +irReturn(irGet(updater))
            return
        }

        val call = irCallConstructor(constructor, typeArguments).apply {
            mappings.forEach { (target, source) ->
                if (target is ValueParameterTarget) {
                    constructArgument(source.single(), target, regularParameters)?.let { argument ->
                        arguments[target.value.indexInParameters] = argument
                    }
                }
            }
        }

        val variable = createTmpVariable(call)

        applySetterMappings(mappings, regularParameters, irGet(variable), skipUpdaterFallbackMappings = false)

        +irReturn(irGet(variable))
    }

    context(context: MappieContext)
    private fun IrBlockBodyBuilder.applySetterMappings(
        mappings: TargetSourcesClassMappings,
        regularParameters: List<IrValueParameter>,
        receiver: IrExpression,
        skipUpdaterFallbackMappings: Boolean,
    ) {
        mappings.forEach { (target, source) ->
            val argumentSource = source.single()
            if (skipUpdaterFallbackMappings && argumentSource.isAutomaticUpdaterFallback()) return@forEach

            when (target) {
                is SetterTarget -> {
                    constructArgument(argumentSource, target, regularParameters)?.let { argument ->
                        +irCall(target.value.setter!!).apply {
                            dispatchReceiver = receiver
                            arguments[1] = argument
                        }
                    }
                }
                is FunctionCallTarget -> {
                    constructArgument(argumentSource, target, regularParameters)?.let { argument ->
                        +irCall(target.value).apply {
                            dispatchReceiver = receiver
                            arguments[1] = argument
                        }
                    }
                }
                else -> {
                    /* Applied as a constructor call argument */
                }
            }
        }
    }

    context(context: MappieContext)
    fun construct(builder: DeclarationIrBuilder): IrCall {
        return builder.irCall(referenceFunctionRun()).apply {
            arguments[0] = builder.irLambda(model.definition.referenceMapFunction().returnType, model.definition.referenceMapFunction().returnType) {
            }
        }
    }

    context(context: MappieContext)
    private fun IrBuilderWithScope.constructArgument(source: ClassMappingSource, target: ClassMappingTarget, parameters: List<IrValueParameter>): IrExpression? =
        when (source) {
            is ExplicitPropertyMappingSource -> {
                val receiver = source.reference.dispatchReceiver
                        ?: irGet(parameters.singleOrNull { it.type == (source.reference.type as IrSimpleType).arguments[0].typeOrFail }
                            ?: panic("Could not determine value parameter for property reference.", source.reference))

                val getter = if (source.forceNonNull) {
                    irCall(referenceFunctionRequireNotNull(), source.reference.getter!!.owner.returnType.makeNotNull()).apply {
                        arguments[0] = irCall(source.reference.getter!!).apply {
                            dispatchReceiver = receiver
                        }
                        arguments[1] = this@constructArgument.irLambda(context.pluginContext.irBuiltIns.anyType, context.pluginContext.irBuiltIns.getKFunctionType(context.pluginContext.irBuiltIns.stringType, emptyList())) {
                            +irReturn(irString("Reference ${source.reference.pretty()} must be non-null."))
                        }
                    }
                } else {
                    irCall(source.reference.getter!!).apply {
                        dispatchReceiver = receiver
                    }
                }
                source.transformation?.let {
                    constructTransformation(model.origin, it, getter, target)
                } ?: getter
            }
            is ExpressionMappingSource -> {
                irCall(referenceFunctionLet()).apply {
                    arguments[0] = irGet(parameters.single())
                    arguments[1] = source.expression
                }
            }
            is ValueMappingSource -> {
                source.expression
            }
            is ImplicitPropertyMappingSource -> {
                val getter = irCall(source.property.getter!!).apply {
                    dispatchReceiver = irGet(parameters.first { it.name == source.parameter })
                }
                source.transformation?.let {
                    constructTransformation(model.origin, it, getter, target)
                } ?: getter
            }
            is FunctionMappingSource -> {
                val call = irCall(source.function.symbol).apply {
                    dispatchReceiver = irGet(parameters.first { it.name == source.parameter })
                }
                source.transformation?.let {
                    constructTransformation(model.origin, it, call, target)
                } ?: call
            }
            is ParameterValueMappingSource -> {
                val getter = irGet(parameters.first { it.name == source.parameter })
                source.transformation?.let {
                    constructTransformation(model.origin, it, getter, target)
                } ?: getter
            }
            is ParameterDefaultValueMappingSource -> {
                null
            }
        }

    private fun ClassMappingSource.isAutomaticUpdaterFallback(): Boolean =
        when (this) {
            is ImplicitPropertyMappingSource -> isFallback
            is FunctionMappingSource -> isFallback
            is ParameterValueMappingSource -> isFallback
            else -> false
        }

    private fun ClassMappingSource.preservesUpdaterValue(): Boolean =
        when (this) {
            is ParameterDefaultValueMappingSource -> true
            is ImplicitPropertyMappingSource -> isFallback && transformation == null
            is FunctionMappingSource -> isFallback && transformation == null
            is ParameterValueMappingSource -> isFallback && transformation == null
            else -> false
        }
}
