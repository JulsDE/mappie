package tech.mappie.testing.objects.update

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tech.mappie.testing.MappieTestCase

class ObjectUpdateMappieTest : MappieTestCase() {

    data class Input(
        val name: String,
    )

    class MutableOutput {
        var nameAssignments = 0
        var ageAssignments = 0

        var name: String = ""
            set(value) {
                nameAssignments++
                field = value
            }

        var age: Int = 0
            set(value) {
                ageAssignments++
                field = value
            }
    }

    data class ConstructorOutput(
        val id: Int,
        var name: String,
    )

    @Test
    fun `update using setters should mutate existing object and keep fallback properties untouched`() {
        compile {
            file("Mapper.kt",
                """
                import tech.mappie.api.ObjectUpdateMappie
                import tech.mappie.testing.objects.update.ObjectUpdateMappieTest.*

                class Mapper : ObjectUpdateMappie<Input, MutableOutput>()
                """
            )
        } satisfies {
            isOk()
            hasNoWarningsOrErrors()

            val mapper = objectUpdateMappie<Input, MutableOutput>()
            val updater = MutableOutput().apply {
                name = "before"
                age = 10
                nameAssignments = 0
                ageAssignments = 0
            }

            val result = mapper.updateFrom(Input("after"), updater)

            assertThat(result).isSameAs(updater)
            assertThat(result.name).isEqualTo("after")
            assertThat(result.age).isEqualTo(10)
            assertThat(result.nameAssignments).isEqualTo(1)
            assertThat(result.ageAssignments).isEqualTo(0)
        }
    }

    @Test
    fun `update constructor backed property should create a new object`() {
        compile {
            file("Mapper.kt",
                """
                import tech.mappie.api.ObjectUpdateMappie
                import tech.mappie.testing.objects.update.ObjectUpdateMappieTest.*

                class Mapper : ObjectUpdateMappie<Input, ConstructorOutput>()
                """
            )
        } satisfies {
            isOk()
            hasNoWarningsOrErrors()

            val mapper = objectUpdateMappie<Input, ConstructorOutput>()
            val updater = ConstructorOutput(1, "before")

            val result = mapper.updateFrom(Input("after"), updater)

            assertThat(result).isNotSameAs(updater)
            assertThat(result).isEqualTo(ConstructorOutput(1, "after"))
            assertThat(updater).isEqualTo(ConstructorOutput(1, "before"))
        }
    }

    @Test
    fun `update using explicit updating dsl should succeed`() {
        compile {
            file("Mapper.kt",
                """
                import tech.mappie.api.ObjectUpdateMappie
                import tech.mappie.testing.objects.update.ObjectUpdateMappieTest.*

                class Mapper : ObjectUpdateMappie<Input, MutableOutput>() {
                    override fun updateFrom(source: Input, updater: MutableOutput) = updating {
                        to::name fromProperty source::name
                    }
                }
                """
            )
        } satisfies {
            isOk()
            hasNoWarningsOrErrors()

            val mapper = objectUpdateMappie<Input, MutableOutput>()
            val updater = MutableOutput().apply {
                name = "before"
                age = 10
            }

            val result = mapper.updateFrom(Input("after"), updater)

            assertThat(result).isSameAs(updater)
            assertThat(result.name).isEqualTo("after")
            assertThat(result.age).isEqualTo(10)
        }
    }
}
