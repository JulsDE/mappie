@file:Suppress("UNUSED_PARAMETER", "SameParameterValue")

package tech.mappie.api

/**
 * Base class for update mappers.
 *
 * Update mappers map [FROM] to an existing [UPDATER] instance.
 *
 * @param FROM the source type to map from.
 * @param UPDATER the target type to update.
 */
public abstract class ObjectUpdateMappie<in FROM, UPDATER> : Mappie<UPDATER> {

    /**
     * Update [updater] using [source].
     *
     * @param source the source value.
     * @param updater the value to update.
     * @return the updated value.
     */
    public open fun updateFrom(source: FROM, updater: UPDATER): UPDATER = generated()

    /**
     * Update nullable [updater] using nullable [source].
     *
     * @param source the source value.
     * @param updater the value to update.
     * @return the updated value.
     */
    public open fun updateFromNullable(source: FROM?, updater: UPDATER?): UPDATER? =
        if (source == null || updater == null) null else updateFrom(source, updater)

    /**
     * Mapping function which instructs Mappie to generate code for this implementation.
     *
     * @param builder the configuration for the generation of this update mapping.
     * @return the updated target value.
     */
    protected fun updating(builder: MultipleObjectMappingConstructor<UPDATER>.() -> Unit = { }): UPDATER = generated()

    /**
     * Mapping function which instructs Mappie to generate code for this implementation.
     *
     * @param builder the configuration for the generation of this update mapping.
     * @return the updated target value.
     */
    protected fun updating(constructor: () -> UPDATER, builder: MultipleObjectMappingConstructor<UPDATER>.() -> Unit = { }): UPDATER = generated()

    /**
     * Mapping function which instructs Mappie to generate code for this implementation.
     *
     * @param P1 The type of the first constructor parameter.
     * @param constructor The specific constructor to call, e.g `::TargetClass`.
     * @param builder the configuration for the generation of this update mapping.
     * @return the updated target value.
     */
    protected fun <P1> updating(constructor: (P1) -> UPDATER, builder: MultipleObjectMappingConstructor<UPDATER>.() -> Unit = { }): UPDATER = generated()

    /**
     * Mapping function which instructs Mappie to generate code for this implementation.
     *
     * @param P1 The type of the first constructor parameter.
     * @param P2 The type of the second constructor parameter.
     * @param constructor The specific constructor to call, e.g `::TargetClass`.
     * @param builder the configuration for the generation of this update mapping.
     * @return the updated target value.
     */
    protected fun <P1, P2> updating(constructor: (P1, P2) -> UPDATER, builder: MultipleObjectMappingConstructor<UPDATER>.() -> Unit = { }): UPDATER = generated()

    /**
     * Mapping function which instructs Mappie to generate code for this implementation.
     *
     * @param P1 The type of the first constructor parameter.
     * @param P2 The type of the second constructor parameter.
     * @param P3 The type of the third constructor parameter.
     * @param constructor The specific constructor to call, e.g `::TargetClass`.
     * @param builder the configuration for the generation of this update mapping.
     * @return the updated target value.
     */
    protected fun <P1, P2, P3> updating(constructor: (P1, P2, P3) -> UPDATER, builder: MultipleObjectMappingConstructor<UPDATER>.() -> Unit = { }): UPDATER = generated()

    /**
     * Mapping function which instructs Mappie to generate code for this implementation.
     *
     * @param P1 The type of the first constructor parameter.
     * @param P2 The type of the second constructor parameter.
     * @param P3 The type of the third constructor parameter.
     * @param P4 The type of the fourth constructor parameter.
     * @param constructor The specific constructor to call, e.g `::TargetClass`.
     * @param builder the configuration for the generation of this update mapping.
     * @return the updated target value.
     */
    protected fun <P1, P2, P3, P4> updating(constructor: (P1, P2, P3, P4) -> UPDATER, builder: MultipleObjectMappingConstructor<UPDATER>.() -> Unit = { }): UPDATER = generated()

    /**
     * Mapping function which instructs Mappie to generate code for this implementation.
     *
     * @param P1 The type of the first constructor parameter.
     * @param P2 The type of the second constructor parameter.
     * @param P3 The type of the third constructor parameter.
     * @param P4 The type of the fourth constructor parameter.
     * @param P5 The type of the fifth constructor parameter.
     * @param constructor The specific constructor to call, e.g `::TargetClass`.
     * @param builder the configuration for the generation of this update mapping.
     * @return the updated target value.
     */
    protected fun <P1, P2, P3, P4, P5> updating(constructor: (P1, P2, P3, P4, P5) -> UPDATER, builder: MultipleObjectMappingConstructor<UPDATER>.() -> Unit = { }): UPDATER = generated()

    /**
     * Mapping function which instructs Mappie to generate code for this implementation.
     *
     * @param P1 The type of the first constructor parameter.
     * @param P2 The type of the second constructor parameter.
     * @param P3 The type of the third constructor parameter.
     * @param P4 The type of the fourth constructor parameter.
     * @param P5 The type of the fifth constructor parameter.
     * @param P6 The type of the sixth constructor parameter.
     * @param constructor The specific constructor to call, e.g `::TargetClass`.
     * @param builder the configuration for the generation of this update mapping.
     * @return the updated target value.
     */
    protected fun <P1, P2, P3, P4, P5, P6> updating(constructor: (P1, P2, P3, P4, P5, P6) -> UPDATER, builder: MultipleObjectMappingConstructor<UPDATER>.() -> Unit = { }): UPDATER = generated()

    /**
     * Mapping function which instructs Mappie to generate code for this implementation.
     *
     * @param P1 The type of the first constructor parameter.
     * @param P2 The type of the second constructor parameter.
     * @param P3 The type of the third constructor parameter.
     * @param P4 The type of the fourth constructor parameter.
     * @param P5 The type of the fifth constructor parameter.
     * @param P6 The type of the sixth constructor parameter.
     * @param P7 The type of the seventh constructor parameter.
     * @param constructor The specific constructor to call, e.g `::TargetClass`.
     * @param builder the configuration for the generation of this update mapping.
     * @return the updated target value.
     */
    protected fun <P1, P2, P3, P4, P5, P6, P7> updating(constructor: (P1, P2, P3, P4, P5, P6, P7) -> UPDATER, builder: MultipleObjectMappingConstructor<UPDATER>.() -> Unit = { }): UPDATER = generated()

    /**
     * Mapping function which instructs Mappie to generate code for this implementation.
     *
     * @param P1 The type of the first constructor parameter.
     * @param P2 The type of the second constructor parameter.
     * @param P3 The type of the third constructor parameter.
     * @param P4 The type of the fourth constructor parameter.
     * @param P5 The type of the fifth constructor parameter.
     * @param P6 The type of the sixth constructor parameter.
     * @param P7 The type of the seventh constructor parameter.
     * @param P8 The type of the eighth constructor parameter.
     * @param constructor The specific constructor to call, e.g `::TargetClass`.
     * @param builder the configuration for the generation of this update mapping.
     * @return the updated target value.
     */
    protected fun <P1, P2, P3, P4, P5, P6, P7, P8> updating(constructor: (P1, P2, P3, P4, P5, P6, P7, P8) -> UPDATER, builder: MultipleObjectMappingConstructor<UPDATER>.() -> Unit = { }): UPDATER = generated()
}
