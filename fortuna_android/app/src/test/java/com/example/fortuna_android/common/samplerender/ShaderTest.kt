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

    @Test
    fun testMatrixArrayMethods() {
        val clazz = Shader::class.java
        val methods = clazz.declaredMethods.map { it.name }.toSet()

        // Test matrix array uniform setters exist
        assertTrue("setMat2Array method should exist", methods.contains("setMat2Array"))
        assertTrue("setMat3Array method should exist", methods.contains("setMat3Array"))
        assertTrue("setMat4Array method should exist", methods.contains("setMat4Array"))

        // Test that these methods return Shader for chaining
        val setMat2ArrayMethod = clazz.declaredMethods.find { it.name == "setMat2Array" }
        assertNotNull("setMat2Array should exist", setMat2ArrayMethod)
        assertEquals("setMat2Array should return Shader", clazz, setMat2ArrayMethod!!.returnType)

        val setMat3ArrayMethod = clazz.declaredMethods.find { it.name == "setMat3Array" }
        assertNotNull("setMat3Array should exist", setMat3ArrayMethod)
        assertEquals("setMat3Array should return Shader", clazz, setMat3ArrayMethod!!.returnType)

        val setMat4ArrayMethod = clazz.declaredMethods.find { it.name == "setMat4Array" }
        assertNotNull("setMat4Array should exist", setMat4ArrayMethod)
        assertEquals("setMat4Array should return Shader", clazz, setMat4ArrayMethod!!.returnType)
    }

    @Test
    fun testSetTextureMethod() {
        val clazz = Shader::class.java
        val methods = clazz.declaredMethods

        val setTextureMethod = methods.find { it.name == "setTexture" }
        assertNotNull("setTexture method should exist", setTextureMethod)

        // Should take String and Texture parameters
        assertEquals("setTexture should take 2 parameters", 2, setTextureMethod!!.parameterCount)
        assertEquals("setTexture should return Shader", clazz, setTextureMethod.returnType)
        assertTrue("setTexture should be public",
            java.lang.reflect.Modifier.isPublic(setTextureMethod.modifiers))
    }

    @Test
    fun testArrayUniformMethods() {
        val clazz = Shader::class.java

        // Test array method parameters and return types
        val arrayMethods = listOf(
            "setBoolArray",
            "setIntArray",
            "setFloatArray",
            "setVec2Array",
            "setVec3Array",
            "setVec4Array"
        )

        arrayMethods.forEach { methodName ->
            val method = clazz.declaredMethods.find { it.name == methodName }
            assertNotNull("$methodName should exist", method)

            if (method != null) {
                assertEquals("$methodName should take 2 parameters", 2, method.parameterCount)
                assertEquals("$methodName should return Shader for chaining", clazz, method.returnType)
                assertTrue("$methodName should be public",
                    java.lang.reflect.Modifier.isPublic(method.modifiers))
            }
        }
    }

    @Test
    fun testPrivateHelperMethods() {
        val clazz = Shader::class.java
        val methods = clazz.declaredMethods

        // Test private helper methods exist
        val getUniformLocationMethod = methods.find { it.name == "getUniformLocation" }
        assertNotNull("getUniformLocation method should exist", getUniformLocationMethod)
        assertTrue("getUniformLocation should be private",
            java.lang.reflect.Modifier.isPrivate(getUniformLocationMethod!!.modifiers))

        val createShaderMethod = methods.find { it.name == "createShader" }
        assertNotNull("createShader method should exist", createShaderMethod)
        assertTrue("createShader should be private",
            java.lang.reflect.Modifier.isPrivate(createShaderMethod!!.modifiers))
        assertTrue("createShader should be static",
            java.lang.reflect.Modifier.isStatic(createShaderMethod.modifiers))

        val createShaderDefinesCodeMethod = methods.find { it.name == "createShaderDefinesCode" }
        assertNotNull("createShaderDefinesCode method should exist", createShaderDefinesCodeMethod)
        assertTrue("createShaderDefinesCode should be private",
            java.lang.reflect.Modifier.isPrivate(createShaderDefinesCodeMethod!!.modifiers))
        assertTrue("createShaderDefinesCode should be static",
            java.lang.reflect.Modifier.isStatic(createShaderDefinesCodeMethod.modifiers))

        val insertShaderDefinesCodeMethod = methods.find { it.name == "insertShaderDefinesCode" }
        assertNotNull("insertShaderDefinesCode method should exist", insertShaderDefinesCodeMethod)
        assertTrue("insertShaderDefinesCode should be private",
            java.lang.reflect.Modifier.isPrivate(insertShaderDefinesCodeMethod!!.modifiers))
        assertTrue("insertShaderDefinesCode should be static",
            java.lang.reflect.Modifier.isStatic(insertShaderDefinesCodeMethod.modifiers))

        val inputStreamToStringMethod = methods.find { it.name == "inputStreamToString" }
        assertNotNull("inputStreamToString method should exist", inputStreamToStringMethod)
        assertTrue("inputStreamToString should be private",
            java.lang.reflect.Modifier.isPrivate(inputStreamToStringMethod!!.modifiers))
        assertTrue("inputStreamToString should be static",
            java.lang.reflect.Modifier.isStatic(inputStreamToStringMethod.modifiers))
    }

    @Test
    fun testUniformInnerClasses() {
        val clazz = Shader::class.java
        val declaredClasses = clazz.declaredClasses

        // Test for specific uniform implementation classes
        val expectedUniformClasses = setOf(
            "UniformTexture", "UniformInt", "Uniform1f", "Uniform2f", "Uniform3f", "Uniform4f",
            "UniformMatrix2f", "UniformMatrix3f", "UniformMatrix4f"
        )

        val actualClasses = declaredClasses.map { it.simpleName }.toSet()

        expectedUniformClasses.forEach { expectedClass ->
            assertTrue("Should have $expectedClass inner class",
                actualClasses.contains(expectedClass))
        }

        // All uniform implementation classes should be private and static
        declaredClasses.filter { it.simpleName.startsWith("Uniform") }.forEach { uniformClass ->
            assertTrue("${uniformClass.simpleName} should be private",
                java.lang.reflect.Modifier.isPrivate(uniformClass.modifiers))
            assertTrue("${uniformClass.simpleName} should be static",
                java.lang.reflect.Modifier.isStatic(uniformClass.modifiers))
        }
    }

    @Test
    fun testShaderConstructorExists() {
        val clazz = Shader::class.java
        val constructors = clazz.declaredConstructors

        // Should have a constructor that takes SampleRender, String, String, Map
        val mainConstructor = constructors.find { it.parameterCount == 4 }
        assertNotNull("Should have 4-parameter constructor", mainConstructor)
        assertTrue("Constructor should be public",
            java.lang.reflect.Modifier.isPublic(mainConstructor!!.modifiers))
    }

    @Test
    fun testBlendMethodOverloads() {
        val clazz = Shader::class.java
        val methods = clazz.declaredMethods

        // Should have two setBlend method overloads
        val blendMethods = methods.filter { it.name == "setBlend" }
        assertEquals("Should have 2 setBlend method overloads", 2, blendMethods.size)

        // One with 2 parameters
        val twoParamBlend = blendMethods.find { it.parameterCount == 2 }
        assertNotNull("Should have 2-parameter setBlend method", twoParamBlend)

        // One with 4 parameters
        val fourParamBlend = blendMethods.find { it.parameterCount == 4 }
        assertNotNull("Should have 4-parameter setBlend method", fourParamBlend)

        // Both should return Shader
        blendMethods.forEach { method ->
            assertEquals("setBlend should return Shader", clazz, method.returnType)
            assertTrue("setBlend should be public",
                java.lang.reflect.Modifier.isPublic(method.modifiers))
        }
    }

    @Test
    fun testShaderFields() {
        val clazz = Shader::class.java
        val fields = clazz.declaredFields

        // Should have private fields for internal state
        val privateFields = fields.filter { java.lang.reflect.Modifier.isPrivate(it.modifiers) }
        assertTrue("Should have private fields", privateFields.isNotEmpty())

        // Test specific important fields
        val programIdField = fields.find { it.name == "programId" }
        assertNotNull("Should have programId field", programIdField)
        if (programIdField != null) {
            assertEquals("programId should be int", Int::class.javaPrimitiveType, programIdField.type)
            assertTrue("programId should be private",
                java.lang.reflect.Modifier.isPrivate(programIdField.modifiers))
        }

        val uniformsField = fields.find { it.name == "uniforms" }
        assertNotNull("Should have uniforms field", uniformsField)
        if (uniformsField != null) {
            assertTrue("uniforms should be private",
                java.lang.reflect.Modifier.isPrivate(uniformsField.modifiers))
            assertTrue("uniforms should be final",
                java.lang.reflect.Modifier.isFinal(uniformsField.modifiers))
        }
    }

    @Test
    fun testDepthStateSetters() {
        val clazz = Shader::class.java

        // Test setDepthTest
        val setDepthTestMethod = clazz.getDeclaredMethod("setDepthTest", Boolean::class.javaPrimitiveType)
        assertNotNull("setDepthTest should exist", setDepthTestMethod)
        assertEquals("setDepthTest should return Shader", clazz, setDepthTestMethod.returnType)
        assertTrue("setDepthTest should be public",
            java.lang.reflect.Modifier.isPublic(setDepthTestMethod.modifiers))

        // Test setDepthWrite
        val setDepthWriteMethod = clazz.getDeclaredMethod("setDepthWrite", Boolean::class.javaPrimitiveType)
        assertNotNull("setDepthWrite should exist", setDepthWriteMethod)
        assertEquals("setDepthWrite should return Shader", clazz, setDepthWriteMethod.returnType)
        assertTrue("setDepthWrite should be public",
            java.lang.reflect.Modifier.isPublic(setDepthWriteMethod.modifiers))
    }

    @Test
    fun testUniformLocationsFields() {
        val clazz = Shader::class.java
        val fields = clazz.declaredFields

        // Should have uniformLocations and uniformNames maps
        val uniformLocationsField = fields.find { it.name == "uniformLocations" }
        assertNotNull("Should have uniformLocations field", uniformLocationsField)
        if (uniformLocationsField != null) {
            assertTrue("uniformLocations should be private",
                java.lang.reflect.Modifier.isPrivate(uniformLocationsField.modifiers))
            assertTrue("uniformLocations should be final",
                java.lang.reflect.Modifier.isFinal(uniformLocationsField.modifiers))
        }

        val uniformNamesField = fields.find { it.name == "uniformNames" }
        assertNotNull("Should have uniformNames field", uniformNamesField)
        if (uniformNamesField != null) {
            assertTrue("uniformNames should be private",
                java.lang.reflect.Modifier.isPrivate(uniformNamesField.modifiers))
            assertTrue("uniformNames should be final",
                java.lang.reflect.Modifier.isFinal(uniformNamesField.modifiers))
        }
    }

    @Test
    fun testBlendStateFields() {
        val clazz = Shader::class.java
        val fields = clazz.declaredFields

        // Should have blend state fields
        val blendFields = listOf("sourceRgbBlend", "destRgbBlend", "sourceAlphaBlend", "destAlphaBlend")

        blendFields.forEach { fieldName ->
            val field = fields.find { it.name == fieldName }
            assertNotNull("Should have $fieldName field", field)
            if (field != null) {
                assertEquals("$fieldName should be BlendFactor type",
                    Shader.BlendFactor::class.java, field.type)
                assertTrue("$fieldName should be private",
                    java.lang.reflect.Modifier.isPrivate(field.modifiers))
            }
        }
    }

    @Test
    fun testStaticTagField() {
        val clazz = Shader::class.java
        val fields = clazz.declaredFields

        val tagField = fields.find { it.name == "TAG" }
        assertNotNull("Should have TAG field", tagField)
        if (tagField != null) {
            assertTrue("TAG should be static",
                java.lang.reflect.Modifier.isStatic(tagField.modifiers))
            assertTrue("TAG should be final",
                java.lang.reflect.Modifier.isFinal(tagField.modifiers))
            assertTrue("TAG should be private",
                java.lang.reflect.Modifier.isPrivate(tagField.modifiers))
        }
    }

    @Test
    fun testUniformInterfaceStructure() {
        val clazz = Shader::class.java
        val declaredClasses = clazz.declaredClasses

        val uniformInterface = declaredClasses.find { it.simpleName == "Uniform" }
        assertNotNull("Uniform interface should exist", uniformInterface)

        if (uniformInterface != null) {
            assertTrue("Uniform should be an interface", uniformInterface.isInterface)
            assertTrue("Uniform should be private",
                java.lang.reflect.Modifier.isPrivate(uniformInterface.modifiers))
            assertTrue("Uniform should be static",
                java.lang.reflect.Modifier.isStatic(uniformInterface.modifiers))

            // Should have use method
            val useMethods = uniformInterface.declaredMethods.filter { it.name == "use" }
            assertTrue("Uniform interface should have use method", useMethods.isNotEmpty())
        }
    }

    @Test
    fun testCreateFromAssetsMethodParameters() {
        val clazz = Shader::class.java
        val methods = clazz.declaredMethods

        val createFromAssetsMethod = methods.find {
            it.name == "createFromAssets" && java.lang.reflect.Modifier.isStatic(it.modifiers)
        }
        assertNotNull("createFromAssets should exist", createFromAssetsMethod)

        if (createFromAssetsMethod != null) {
            assertEquals("createFromAssets should take 4 parameters",
                4, createFromAssetsMethod.parameterCount)
            assertEquals("createFromAssets should return Shader",
                clazz, createFromAssetsMethod.returnType)
            assertTrue("createFromAssets should be public",
                java.lang.reflect.Modifier.isPublic(createFromAssetsMethod.modifiers))
            assertTrue("createFromAssets should be static",
                java.lang.reflect.Modifier.isStatic(createFromAssetsMethod.modifiers))
        }
    }

    @Test
    fun testAllUniformSetterMethods() {
        val clazz = Shader::class.java
        val methods = clazz.declaredMethods.map { it.name }.toSet()

        // All uniform setter methods should exist
        val expectedSetterMethods = setOf(
            "setBool", "setInt", "setFloat", "setVec2", "setVec3", "setVec4",
            "setMat2", "setMat3", "setMat4", "setBoolArray", "setIntArray",
            "setFloatArray", "setVec2Array", "setVec3Array", "setVec4Array",
            "setMat2Array", "setMat3Array", "setMat4Array", "setTexture"
        )

        expectedSetterMethods.forEach { methodName ->
            assertTrue("$methodName should exist", methods.contains(methodName))
        }
    }
}