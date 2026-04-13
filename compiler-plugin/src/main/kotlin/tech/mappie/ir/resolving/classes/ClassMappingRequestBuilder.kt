package tech.mappie.ir.resolving.classes

import org.jetbrains.kotlin.backend.jvm.ir.upperBound
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.ifEmpty
import tech.mappie.ir.MappieContext
import tech.mappie.config.options.NamingConventionMode
import tech.mappie.config.options.namingConvention
import tech.mappie.config.options.useDefaultArguments
import tech.mappie.exceptions.MappiePanicException.Companion.panic
import tech.mappie.ir.InternalMappieDefinition
import tech.mappie.ir.PrioritizationMap.Companion.prioritize
import tech.mappie.ir.resolving.*
import tech.mappie.ir.resolving.classes.sources.*
import tech.mappie.ir.resolving.classes.sources.FunctionMappingSource
import tech.mappie.ir.resolving.classes.sources.ImplicitClassMappingSource
import tech.mappie.ir.resolving.classes.sources.ImplicitPropertyMappingSource
import tech.mappie.ir.resolving.classes.sources.ParameterDefaultValueMappingSource
import tech.mappie.ir.resolving.classes.sources.ParameterValueMappingSource
import tech.mappie.ir.resolving.classes.sources.PropertyMappingViaLocalMethodTransformation
import tech.mappie.ir.resolving.classes.targets.ClassMappingTarget
import tech.mappie.ir.resolving.classes.targets.ValueParameterTarget
import tech.mappie.ir.analysis.Problem
import tech.mappie.ir.util.isPrimitive
import tech.mappie.ir.util.isSubtypeOf
import tech.mappie.ir.util.location
import tech.mappie.util.normalize

class ClassMappingRequestBuilder(private val constructor: IrConstructor) {

    private val targets = mutableListOf<ClassMappingTarget>()

    private val sources = mutableMapOf<Name, IrType>()

    private val implicit = mutableMapOf<Name, List<ImplicitClassMappingSource>>()

    private val fallbackImplicit = mutableMapOf<Name, List<ImplicitClassMappingSource>>()

    private val explicit = mutableMapOf<Name, List<ExplicitClassMappingSource>>()

    context(context: MappieContext)
    fun construct(origin: InternalMappieDefinition): ClassMappingRequest {
        val useDefaultArguments = useDefaultArguments(origin.referenceMapFunction())
        val namingConvention = namingConvention(origin.referenceMapFunction())

        val normalizedImplicit = if (namingConvention == NamingConventionMode.LENIENT) implicit.buildNormalizedLookup() else null
        val normalizedFallbackImplicit = if (namingConvention == NamingConventionMode.LENIENT) fallbackImplicit.buildNormalizedLookup() else null

        val mappings = targets.associateWith { target ->
            explicit(origin, target) ?: implicit(origin, target, useDefaultArguments, normalizedImplicit, normalizedFallbackImplicit)
        }

        return ClassMappingRequest(origin, sources.map { it.value }, constructor, TargetSourcesClassMappings(mappings))
    }

    private fun Map<Name, List<ImplicitClassMappingSource>>.buildNormalizedLookup(): Map<String, List<ImplicitClassMappingSource>> =
        flatMap { (name, sources) ->
            sources.map { source -> name.normalize() to source }
        }.groupBy({ it.first }, { it.second })

    context(context: MappieContext)
    private fun explicit(origin: InternalMappieDefinition, target: ClassMappingTarget): List<ExplicitClassMappingSource>? =
        explicit[target.name]?.let { sources ->
            sources.map { source ->
                if (source is ExplicitPropertyMappingSource && source.transformation == null && !target.type.isSubtypeOf(source.type)) {
                    source.copy(transformation = transformation(origin, source, target))
                } else {
                    source
                }
            }
        }

    context(context: MappieContext)
    private fun implicit(
        origin: InternalMappieDefinition,
        target: ClassMappingTarget,
        useDefaultArguments: Boolean,
        normalizedImplicit: Map<String, List<ImplicitClassMappingSource>>?,
        normalizedFallbackImplicit: Map<String, List<ImplicitClassMappingSource>>?,
    ): List<ImplicitClassMappingSource> {
        val sources = sources(implicit, target, normalizedImplicit)
        val fallbackSources = sources(fallbackImplicit, target, normalizedFallbackImplicit)

        return resolvedSources(origin, target, sources)
            .ifEmpty {
                resolvedSources(origin, target, fallbackSources)
            }
            .ifEmpty {
                if (target is ValueParameterTarget && target.value.hasDefaultValue() && useDefaultArguments) {
                    listOf(ParameterDefaultValueMappingSource(target.value))
                } else {
                    emptyList()
                }
            }
    }

    private fun sources(
        implicit: Map<Name, List<ImplicitClassMappingSource>>,
        target: ClassMappingTarget,
        normalizedImplicit: Map<String, List<ImplicitClassMappingSource>>?,
    ): List<ImplicitClassMappingSource> {
        val exactMatch = implicit.getOrDefault(target.name, emptyList())
        return if (exactMatch.isNotEmpty() || normalizedImplicit == null) {
            exactMatch
        } else {
            normalizedImplicit.getOrDefault(target.name.normalize(), emptyList())
        }
    }

    context(context: MappieContext)
    private fun resolvedSources(
        origin: InternalMappieDefinition,
        target: ClassMappingTarget,
        sources: List<ImplicitClassMappingSource>,
    ): List<ImplicitClassMappingSource> =
        sources.filter { it.type.isSubtypeOf(target.type) }
            .ifEmpty {
                sources.map { source ->
                    when (source) {
                        is ImplicitPropertyMappingSource -> source.copy(transformation = transformation(origin, source, target))
                        is FunctionMappingSource -> source.copy(transformation = transformation(origin, source, target))
                        is ParameterValueMappingSource -> source.copy(transformation = transformation(origin, source, target))
                        is ParameterDefaultValueMappingSource -> panic("ParameterDefaultValueMappingSource should not occur when resolving a transformation.")
                    }
                }
            }

    context(context: MappieContext)
    private fun transformation(origin: InternalMappieDefinition, source: ClassMappingSource, target: ClassMappingTarget): PropertyMappingTransformation? {
        // 1. Check local conversion methods first (highest priority)
        val localMethod = origin.localConversions.matching(source.type, target.type).firstOrNull()
        if (localMethod != null) {
            return PropertyMappingViaLocalMethodTransformation(localMethod, target.type)
        }

        // 2. Fall back to existing mapper lookup
        val mappers = context.definitions.matching(origin, source.type, target.type)

        val prioritized = mappers.prioritize(source.type, target.type)
        val selected = prioritized.select()

        return when {
            selected != null -> {
                PropertyMappingViaMapperTransformation(selected, null, target.type)
            }
            prioritized.size > 1 -> {
                val location = when (source) {
                    is ExplicitClassMappingSource -> location(origin.referenceMapFunction().fileEntry, source.origin)
                    else -> location(origin.referenceMapFunction())
                }
                val error = Problem.error("Multiple mappers resolved to be used in an implicit via", location)
                context.logger.log(error)
                null
            }
            !source.type.isPrimitive() && !target.type.isPrimitive() -> {
                GeneratedViaMapperTransformation(source, target)
            }
            else -> {
                null
            }
        }
    }

    fun explicit(entry: Pair<Name, ExplicitClassMappingSource>): ClassMappingRequestBuilder = apply {
        explicit.merge(entry.first, listOf(entry.second), List<ExplicitClassMappingSource>::plus)
    }

    context(context: MappieContext)
    fun sources(parameters: List<Pair<Name, IrType>>) = apply {
        sources.putAll(parameters)
        parameters.map { (name, type) ->
            implicit.merge(name, listOf(ParameterValueMappingSource(name, type, null)), List<ImplicitClassMappingSource>::plus)
            type.upperBound.getClass()!!.accept(ImplicitClassMappingSourcesCollector(context), name to type).forEach { (name, source) ->
                implicit.merge(name, listOf(source), List<ImplicitClassMappingSource>::plus)
            }
        }
    }

    context(context: MappieContext)
    fun fallbackSources(parameters: List<Pair<Name, IrType>>) = apply {
        parameters.forEach { (name, type) ->
            type.upperBound.getClass()!!.accept(ImplicitClassMappingSourcesCollector(context), name to type).forEach { (sourceName, source) ->
                val fallback = when (source) {
                    is ImplicitPropertyMappingSource -> source.copy(isFallback = true)
                    is FunctionMappingSource -> source.copy(isFallback = true)
                    is ParameterValueMappingSource -> source.copy(isFallback = true)
                    is ParameterDefaultValueMappingSource -> source
                }
                fallbackImplicit.merge(sourceName, listOf(fallback), List<ImplicitClassMappingSource>::plus)
            }
        }
    }

    fun targets(targets: List<ClassMappingTarget>) = apply {
        this.targets.addAll(targets)
    }
}
