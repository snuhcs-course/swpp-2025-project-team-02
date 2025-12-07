package com.example.fortuna_android.common.samplerender

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShaderTest {

    @Test
    fun testBlendFactorEnumValues() {
        // Test that BlendFactor enum values match expected OpenGL constants
        assertEquals(0x0000, Shader.BlendFactor.ZERO.glesEnum)
        assertEquals(0x0001, Shader.BlendFactor.ONE.glesEnum)
        assertEquals(0x0300, Shader.BlendFactor.SRC_COLOR.glesEnum)
        assertEquals(0x0301, Shader.BlendFactor.ONE_MINUS_SRC_COLOR.glesEnum)
        assertEquals(0x0306, Shader.BlendFactor.DST_COLOR.glesEnum)
        assertEquals(0x0307, Shader.BlendFactor.ONE_MINUS_DST_COLOR.glesEnum)
        assertEquals(0x0302, Shader.BlendFactor.SRC_ALPHA.glesEnum)
        assertEquals(0x0303, Shader.BlendFactor.ONE_MINUS_SRC_ALPHA.glesEnum)
        assertEquals(0x0304, Shader.BlendFactor.DST_ALPHA.glesEnum)
        assertEquals(0x0305, Shader.BlendFactor.ONE_MINUS_DST_ALPHA.glesEnum)
        assertEquals(0x8001, Shader.BlendFactor.CONSTANT_COLOR.glesEnum)
        assertEquals(0x8002, Shader.BlendFactor.ONE_MINUS_CONSTANT_COLOR.glesEnum)
        assertEquals(0x8003, Shader.BlendFactor.CONSTANT_ALPHA.glesEnum)
        assertEquals(0x8004, Shader.BlendFactor.ONE_MINUS_CONSTANT_ALPHA.glesEnum)
    }

    @Test
    fun testBlendFactorEnumCompleteness() {
        // Ensure all expected blend factors are present
        val blendFactors = Shader.BlendFactor.values()
        assertEquals("Should have 14 blend factors", 14, blendFactors.size)

        // Test that each enum value has a unique glesEnum
        val glesEnums = blendFactors.map { it.glesEnum }.toSet()
        assertEquals("All glesEnum values should be unique", blendFactors.size, glesEnums.size)
    }

    @Test
    fun testShaderClassStructure() {
        // Test basic class structure
        val clazz = Shader::class.java

        // Should be a public class
        assertTrue("Class should be public", java.lang.reflect.Modifier.isPublic(clazz.modifiers))

        // Should implement Closeable
        assertTrue("Should implement Closeable", java.io.Closeable::class.java.isAssignableFrom(clazz))
    }

    @Test
    fun testShaderMethods() {
        // Test that required methods exist
        val clazz = Shader::class.java
        val methods = clazz.declaredMethods

        // Check for essential setter methods
        val setBlendMethod = methods.find { it.name == "setBlend" && it.parameterCount == 2 }
        assertNotNull("setBlend(BlendFactor, BlendFactor) method should exist", setBlendMethod)

        val setDepthTestMethod = methods.find { it.name == "setDepthTest" }
        assertNotNull("setDepthTest method should exist", setDepthTestMethod)

        val setDepthWriteMethod = methods.find { it.name == "setDepthWrite" }
        assertNotNull("setDepthWrite method should exist", setDepthWriteMethod)

        val setBoolMethod = methods.find { it.name == "setBool" }
        assertNotNull("setBool method should exist", setBoolMethod)

        val setIntMethod = methods.find { it.name == "setInt" }
        assertNotNull("setInt method should exist", setIntMethod)

        val setFloatMethod = methods.find { it.name == "setFloat" }
        assertNotNull("setFloat method should exist", setFloatMethod)

        val closeMethod = methods.find { it.name == "close" }
        assertNotNull("close method should exist", closeMethod)
    }

    @Test
    fun testUniformSetterMethodsExist() {
        val clazz = Shader::class.java
        val methods = clazz.declaredMethods.map { it.name }.toSet()

        // Test vector uniform setters
        assertTrue("setVec2 method should exist", methods.contains("setVec2"))
        assertTrue("setVec3 method should exist", methods.contains("setVec3"))
        assertTrue("setVec4 method should exist", methods.contains("setVec4"))

        // Test matrix uniform setters
        assertTrue("setMat2 method should exist", methods.contains("setMat2"))
        assertTrue("setMat3 method should exist", methods.contains("setMat3"))
        assertTrue("setMat4 method should exist", methods.contains("setMat4"))

        // Test array uniform setters
        assertTrue("setBoolArray method should exist", methods.contains("setBoolArray"))
        assertTrue("setIntArray method should exist", methods.contains("setIntArray"))
        assertTrue("setFloatArray method should exist", methods.contains("setFloatArray"))
        assertTrue("setVec2Array method should exist", methods.contains("setVec2Array"))
        assertTrue("setVec3Array method should exist", methods.contains("setVec3Array"))
        assertTrue("setVec4Array method should exist", methods.contains("setVec4Array"))
    }

    @Test
    fun testStaticFactoryMethod() {
        val clazz = Shader::class.java
        val methods = clazz.declaredMethods

        val createFromAssetsMethod = methods.find {
            it.name == "createFromAssets" &&
            java.lang.reflect.Modifier.isStatic(it.modifiers)
        }
        assertNotNull("createFromAssets static method should exist", createFromAssetsMethod)
        assertEquals("createFromAssets should return Shader", clazz, createFromAssetsMethod!!.returnType)
    }

    @Test
    fun testBlendFactorPrivateConstructor() {
        val constructors = Shader.BlendFactor::class.java.declaredConstructors
        assertTrue("BlendFactor should have constructors", constructors.isNotEmpty())

        // All constructors should be private for enum
        constructors.forEach { constructor ->
            assertTrue("Enum constructor should be private",
                java.lang.reflect.Modifier.isPrivate(constructor.modifiers))
        }
    }

    @Test
    fun testMethodReturnTypes() {
        val clazz = Shader::class.java

        // Methods that should return Shader for method chaining
        val chainingMethods = listOf(
            "setBlend", "setDepthTest", "setDepthWrite", "setBool", "setInt",
            "setFloat", "setVec2", "setVec3", "setVec4", "setMat2", "setMat3", "setMat4"
        )

        chainingMethods.forEach { methodName ->
            val methods = clazz.declaredMethods.filter { it.name == methodName }
            assertTrue("$methodName methods should exist", methods.isNotEmpty())

            methods.forEach { method ->
                assertEquals("$methodName should return Shader for chaining",
                    clazz, method.returnType)
            }
        }
    }

    @Test
    fun testLowLevelUseMethod() {
        val clazz = Shader::class.java
        val lowLevelUseMethod = clazz.declaredMethods.find { it.name == "lowLevelUse" }

        assertNotNull("lowLevelUse method should exist", lowLevelUseMethod)
        assertEquals("lowLevelUse should return void", Void.TYPE, lowLevelUseMethod!!.returnType)
        assertTrue("lowLevelUse should be public",
            java.lang.reflect.Modifier.isPublic(lowLevelUseMethod.modifiers))
    }

    @Test
    fun testInnerUniformClasses() {
        val clazz = Shader::class.java
        val declaredClasses = clazz.declaredClasses

        // Should have various uniform implementation classes
        assertTrue("Should have inner classes for uniforms", declaredClasses.isNotEmpty())

        // Check for Uniform interface
        val uniformInterface = declaredClasses.find { it.simpleName == "Uniform" }
        assertNotNull("Uniform interface should exist", uniformInterface)
        assertTrue("Uniform should be an interface", uniformInterface!!.isInterface)
    }
}