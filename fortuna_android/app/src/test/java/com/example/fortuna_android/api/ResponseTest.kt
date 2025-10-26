package com.example.fortuna_android.api

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Response.kt data classes
 * Tests all API response models for correct data handling
 */
class ResponseTest {

    // ServerResponse abstract class - test via concrete implementation
    private class TestServerResponse(
        override val status: Int,
        override val data: String
    ) : ServerResponse<String>()

    @Test
    fun `test ServerResponse concrete implementation`() {
        val response = TestServerResponse(200, "test data")
        assertEquals(200, response.status)
        assertEquals("test data", response.data)
    }

    // UploadUrlData tests
    @Test
    fun `test UploadUrlData creation`() {
        val data = UploadUrlData(
            uploadUrl = "https://s3.amazonaws.com/bucket/key",
            key = "unique-key-123",
            fileId = "file-456",
            expiresIn = 3600
        )

        assertEquals("https://s3.amazonaws.com/bucket/key", data.uploadUrl)
        assertEquals("unique-key-123", data.key)
        assertEquals("file-456", data.fileId)
        assertEquals(3600, data.expiresIn)
    }

    @Test
    fun `test UploadUrlData equality`() {
        val data1 = UploadUrlData("url", "key", "fileId", 3600)
        val data2 = UploadUrlData("url", "key", "fileId", 3600)

        assertEquals(data1, data2)
        assertEquals(data1.hashCode(), data2.hashCode())
    }

    @Test
    fun `test UploadUrlData copy`() {
        val original = UploadUrlData("url", "key", "fileId", 3600)
        val copy = original.copy(expiresIn = 7200)

        assertEquals("url", copy.uploadUrl)
        assertEquals(7200, copy.expiresIn)
    }

    // ImageResponse, ImageData, ImageItem tests
    @Test
    fun `test ImageItem creation`() {
        val item = ImageItem(
            filename = "image.jpg",
            path = "/path/to/image.jpg",
            url = "https://example.com/image.jpg"
        )

        assertEquals("image.jpg", item.filename)
        assertEquals("/path/to/image.jpg", item.path)
        assertEquals("https://example.com/image.jpg", item.url)
    }

    @Test
    fun `test ImageData creation`() {
        val items = listOf(
            ImageItem("img1.jpg", "/path1", "url1"),
            ImageItem("img2.jpg", "/path2", "url2")
        )
        val imageData = ImageData(
            date = "2025-10-24",
            images = items,
            count = 2
        )

        assertEquals("2025-10-24", imageData.date)
        assertEquals(2, imageData.images.size)
        assertEquals(2, imageData.count)
    }

    @Test
    fun `test ImageResponse creation`() {
        val imageData = ImageData("2025-10-24", emptyList(), 0)
        val response = ImageResponse(
            status = "success",
            data = imageData
        )

        assertEquals("success", response.status)
        assertNotNull(response.data)
    }

    // UploadResponse and UploadData tests
    @Test
    fun `test UploadData creation with all fields`() {
        val data = UploadData(
            filename = "photo.jpg",
            url = "https://example.com/photo.jpg",
            chakraType = "wood"
        )

        assertEquals("photo.jpg", data.filename)
        assertEquals("https://example.com/photo.jpg", data.url)
        assertEquals("wood", data.chakraType)
    }

    @Test
    fun `test UploadData creation with null fields`() {
        val data = UploadData(
            filename = null,
            url = null,
            chakraType = null
        )

        assertNull(data.filename)
        assertNull(data.url)
        assertNull(data.chakraType)
    }

    @Test
    fun `test UploadResponse with null data`() {
        val response = UploadResponse(
            status = "error",
            data = null
        )

        assertEquals("error", response.status)
        assertNull(response.data)
    }

    @Test
    fun `test UploadResponse with valid data`() {
        val uploadData = UploadData("file.jpg", "url", "fire")
        val response = UploadResponse("success", uploadData)

        assertEquals("success", response.status)
        assertNotNull(response.data)
        assertEquals("file.jpg", response.data?.filename)
    }

    // TomorrowGapja tests
    @Test
    fun `test TomorrowGapja creation`() {
        val gapja = TomorrowGapja(
            code = 1,
            name = "갑자",
            element = "wood"
        )

        assertEquals(1, gapja.code)
        assertEquals("갑자", gapja.name)
        assertEquals("wood", gapja.element)
    }

    // Fortune and related tests
    @Test
    fun `test Fortune creation`() {
        val dailyGuidance = DailyGuidance(
            bestTime = "오전",
            keyAdvice = "조심하세요",
            luckyColor = "빨강",
            luckyDirection = "동쪽",
            activitiesToAvoid = listOf("과음"),
            activitiesToEmbrace = listOf("운동")
        )

        val chakraReadings = listOf(
            ChakraReading("강합니다", 8, "wood", "동쪽")
        )

        val fortune = Fortune(
            tomorrowDate = "2025-10-25",
            dailyGuidance = dailyGuidance,
            chakraReadings = chakraReadings,
            elementBalance = "균형",
            fortuneSummary = "좋음",
            overallFortune = 85,
            specialMessage = "특별한 메시지",
            sajuCompatibility = "궁합 좋음"
        )

        assertEquals("2025-10-25", fortune.tomorrowDate)
        assertEquals(85, fortune.overallFortune)
        assertEquals("균형", fortune.elementBalance)
    }

    @Test
    fun `test ChakraReading creation`() {
        val reading = ChakraReading(
            message = "기운이 좋습니다",
            strength = 9,
            chakraType = "fire",
            locationSignificance = "남쪽"
        )

        assertEquals("기운이 좋습니다", reading.message)
        assertEquals(9, reading.strength)
        assertEquals("fire", reading.chakraType)
        assertEquals("남쪽", reading.locationSignificance)
    }

    @Test
    fun `test DailyGuidance creation`() {
        val guidance = DailyGuidance(
            bestTime = "오후 2시",
            keyAdvice = "신중하게",
            luckyColor = "파랑",
            luckyDirection = "서쪽",
            activitiesToAvoid = listOf("다툼", "지출"),
            activitiesToEmbrace = listOf("독서", "명상")
        )

        assertEquals("오후 2시", guidance.bestTime)
        assertEquals("신중하게", guidance.keyAdvice)
        assertEquals(2, guidance.activitiesToAvoid.size)
        assertEquals(2, guidance.activitiesToEmbrace.size)
    }

    @Test
    fun `test DailyGuidance with empty lists`() {
        val guidance = DailyGuidance(
            bestTime = "언제든",
            keyAdvice = "자유롭게",
            luckyColor = "흰색",
            luckyDirection = "어디든",
            activitiesToAvoid = emptyList(),
            activitiesToEmbrace = emptyList()
        )

        assertTrue(guidance.activitiesToAvoid.isEmpty())
        assertTrue(guidance.activitiesToEmbrace.isEmpty())
    }

    @Test
    fun `test FortuneData creation`() {
        val fortune = Fortune(
            tomorrowDate = "2025-10-25",
            dailyGuidance = DailyGuidance("오전", "조심", "빨강", "동쪽", emptyList(), emptyList()),
            chakraReadings = emptyList(),
            elementBalance = "균형",
            fortuneSummary = "좋음",
            overallFortune = 80,
            specialMessage = "메시지",
            sajuCompatibility = "좋음"
        )

        val gapja = TomorrowGapja(1, "갑자", "wood")

        val fortuneData = FortuneData(
            fortuneId = 123,
            userId = 456,
            forDate = "2025-10-25",
            fortuneStatus = "completed",
            fortune = fortune,
            createdAt = "2025-10-24T10:00:00Z",
            updatedAt = "2025-10-24T11:00:00Z",
            tomorrowGapja = gapja
        )

        assertEquals(123, fortuneData.fortuneId)
        assertEquals(456, fortuneData.userId)
        assertEquals("completed", fortuneData.fortuneStatus)
        assertNotNull(fortuneData.fortune)
        assertNotNull(fortuneData.tomorrowGapja)
    }

    @Test
    fun `test FortuneResponse creation`() {
        val fortune = Fortune(
            "2025-10-25",
            DailyGuidance("오전", "조심", "빨강", "동쪽", emptyList(), emptyList()),
            emptyList(),
            "균형", "좋음", 80, "메시지", "좋음"
        )
        val fortuneData = FortuneData(
            1, 2, "2025-10-25", "completed", fortune,
            "2025-10-24T10:00:00Z", "2025-10-24T11:00:00Z",
            TomorrowGapja(1, "갑자", "wood")
        )
        val response = FortuneResponse("success", fortuneData)

        assertEquals("success", response.status)
        assertNotNull(response.data)
    }

    // Authentication related tests
    @Test
    fun `test LoginRequest creation`() {
        val request = LoginRequest("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", request.idToken)
    }

    @Test
    fun `test LoginResponse creation`() {
        val response = LoginResponse(
            accessToken = "access-token-123",
            refreshToken = "refresh-token-456",
            userId = "user-789",
            email = "test@example.com",
            name = "Test User",
            profileImage = "https://example.com/profile.jpg",
            isNewUser = true,
            needsAdditionalInfo = false
        )

        assertEquals("access-token-123", response.accessToken)
        assertEquals("refresh-token-456", response.refreshToken)
        assertEquals("user-789", response.userId)
        assertEquals("test@example.com", response.email)
        assertTrue(response.isNewUser)
        assertFalse(response.needsAdditionalInfo)
    }

    @Test
    fun `test LoginResponse for existing user`() {
        val response = LoginResponse(
            "token1", "token2", "id", "email@test.com",
            "Name", "image.jpg", false, false
        )

        assertFalse(response.isNewUser)
        assertFalse(response.needsAdditionalInfo)
    }

    @Test
    fun `test User creation`() {
        val user = User(
            id = 1,
            username = "testuser",
            email = "test@example.com"
        )

        assertEquals(1, user.id)
        assertEquals("testuser", user.username)
        assertEquals("test@example.com", user.email)
    }

    @Test
    fun `test UserProfile creation with all fields`() {
        val profile = UserProfile(
            userId = 1,
            email = "test@example.com",
            name = "Test User",
            profileImage = "https://example.com/profile.jpg",
            nickname = "tester",
            birthDateSolar = "1990-01-01",
            birthDateLunar = "1989-12-15",
            solarOrLunar = "solar",
            birthTimeUnits = "자시",
            gender = "M",
            yearlyGanji = "경오",
            monthlyGanji = "무인",
            dailyGanji = "갑자",
            hourlyGanji = "병자",
            createdAt = "2025-01-01T00:00:00Z",
            lastLogin = "2025-10-24T10:00:00Z"
        )

        assertEquals(1, profile.userId)
        assertEquals("test@example.com", profile.email)
        assertEquals("tester", profile.nickname)
        assertEquals("solar", profile.solarOrLunar)
    }

    @Test
    fun `test UserProfile creation with null fields`() {
        val profile = UserProfile(
            userId = 1,
            email = "test@example.com",
            name = "Test User",
            profileImage = null,
            nickname = null,
            birthDateSolar = null,
            birthDateLunar = null,
            solarOrLunar = null,
            birthTimeUnits = null,
            gender = null,
            yearlyGanji = null,
            monthlyGanji = null,
            dailyGanji = null,
            hourlyGanji = null,
            createdAt = null,
            lastLogin = null
        )

        assertEquals(1, profile.userId)
        assertNull(profile.nickname)
        assertNull(profile.birthDateSolar)
    }

    @Test
    fun `test UpdateProfileRequest creation`() {
        val request = UpdateProfileRequest(
            nickname = "newname",
            inputBirthDate = "1990-01-01",
            inputCalendarType = "solar",
            birthTimeUnits = "자시",
            gender = "F"
        )

        assertEquals("newname", request.nickname)
        assertEquals("1990-01-01", request.inputBirthDate)
        assertEquals("solar", request.inputCalendarType)
        assertEquals("F", request.gender)
    }

    @Test
    fun `test UpdatedUserData creation`() {
        val data = UpdatedUserData(
            userId = 1,
            email = "test@example.com",
            name = "Test User",
            nickname = "tester",
            birthDateSolar = "1990-01-01",
            birthDateLunar = "1989-12-15",
            solarOrLunar = "solar",
            birthTimeUnits = "자시",
            gender = "M",
            yearlyGanji = "경오",
            monthlyGanji = "무인",
            dailyGanji = "갑자",
            hourlyGanji = "병자"
        )

        assertEquals(1, data.userId)
        assertEquals("tester", data.nickname)
        assertEquals("solar", data.solarOrLunar)
    }

    @Test
    fun `test UpdateProfileResponse creation`() {
        val userData = UpdatedUserData(
            1, "email", "name", "nick", "1990-01-01", "1989-12-15",
            "solar", "자시", "M", "경오", "무인", "갑자", "병자"
        )
        val response = UpdateProfileResponse(
            message = "Profile updated successfully",
            user = userData
        )

        assertEquals("Profile updated successfully", response.message)
        assertNotNull(response.user)
    }

    @Test
    fun `test LogoutRequest creation`() {
        val request = LogoutRequest("refresh-token-123")
        assertEquals("refresh-token-123", request.refreshToken)
    }

    @Test
    fun `test LogoutResponse creation`() {
        val response = LogoutResponse("Successfully logged out")
        assertEquals("Successfully logged out", response.message)
    }

    @Test
    fun `test LogoutResponse with null message`() {
        val response = LogoutResponse(null)
        assertNull(response.message)
    }

    @Test
    fun `test RefreshTokenRequest creation`() {
        val request = RefreshTokenRequest("old-refresh-token")
        assertEquals("old-refresh-token", request.refresh)
    }

    @Test
    fun `test RefreshTokenResponse creation`() {
        val response = RefreshTokenResponse("new-access-token")
        assertEquals("new-access-token", response.access)
    }

    // Today Fortune related tests
    @Test
    fun `test TodayFortuneResponse creation`() {
        val fortuneData = TodayFortuneData(
            fortuneId = 1,
            userId = 1,
            generatedAt = "2025-10-24T10:00:00Z",
            forDate = "2025-10-24",
            fortune = TodayFortune(
                "궁합", 85, "좋음", "균형",
                emptyList(),
                DailyGuidance("오전", "조심", "빨강", "동쪽", emptyList(), emptyList()),
                "특별메시지"
            ),
            fortuneScore = FortuneScore(0.75, emptyMap(), emptyMap(), "해석")
        )

        val response = TodayFortuneResponse("success", fortuneData)

        assertEquals("success", response.status)
        assertNotNull(response.data)
    }

    @Test
    fun `test TodayFortuneData with null fields`() {
        val data = TodayFortuneData(
            fortuneId = 1,
            userId = 1,
            generatedAt = null,
            forDate = null,
            fortune = TodayFortune(
                "궁합", 85, "좋음", "균형",
                emptyList(),
                DailyGuidance("오전", "조심", "빨강", "동쪽", emptyList(), emptyList()),
                "메시지"
            ),
            fortuneScore = FortuneScore(0.75, emptyMap(), emptyMap(), "해석")
        )

        assertNull(data.generatedAt)
        assertNull(data.forDate)
    }

    @Test
    fun `test TodayFortune creation`() {
        val fortune = TodayFortune(
            sajuCompatibility = "매우 좋음",
            overallFortune = 90,
            fortuneSummary = "행운의 날",
            elementBalance = "완벽한 균형",
            chakraReadings = listOf(
                ChakraReading("강함", 10, "wood", "동쪽"),
                ChakraReading("중간", 5, "fire", "남쪽")
            ),
            dailyGuidance = DailyGuidance(
                "오전 10시", "적극적으로", "금색", "북쪽",
                listOf("싸움"), listOf("대화", "협력")
            ),
            specialMessage = "오늘은 특별한 날입니다"
        )

        assertEquals(90, fortune.overallFortune)
        assertEquals("매우 좋음", fortune.sajuCompatibility)
        assertEquals(2, fortune.chakraReadings.size)
    }

    @Test
    fun `test FortuneScore creation`() {
        val pillar = ElementPillar(
            twoLetters = "갑자",
            stem = StemBranchDetail("갑", "wood", "#0BEFA0", "양", null),
            branch = StemBranchDetail("자", "water", "#2BB3FC", "양", "쥐")
        )

        val distribution = ElementDistribution(count = 3, percentage = 37.5)

        val score = FortuneScore(
            entropyScore = 0.85,
            elements = mapOf("일운" to pillar),
            elementDistribution = mapOf("목" to distribution),
            interpretation = "균형잡힌 운세"
        )

        assertEquals(0.85, score.entropyScore, 0.001)
        assertEquals(1, score.elements.size)
        assertEquals(1, score.elementDistribution.size)
        assertEquals("균형잡힌 운세", score.interpretation)
    }

    @Test
    fun `test ElementPillar creation`() {
        val pillar = ElementPillar(
            twoLetters = "갑자",
            stem = StemBranchDetail("갑", "wood", "#0BEFA0", "양", null),
            branch = StemBranchDetail("자", "water", "#2BB3FC", "양", "쥐")
        )

        assertEquals("갑자", pillar.twoLetters)
        assertNotNull(pillar.stem)
        assertNotNull(pillar.branch)
    }

    @Test
    fun `test StemBranchDetail with animal`() {
        val detail = StemBranchDetail(
            koreanName = "자",
            element = "water",
            elementColor = "#2BB3FC",
            yinYang = "양",
            animal = "쥐"
        )

        assertEquals("자", detail.koreanName)
        assertEquals("water", detail.element)
        assertEquals("#2BB3FC", detail.elementColor)
        assertEquals("양", detail.yinYang)
        assertEquals("쥐", detail.animal)
    }

    @Test
    fun `test StemBranchDetail without animal`() {
        val detail = StemBranchDetail(
            koreanName = "갑",
            element = "wood",
            elementColor = "#0BEFA0",
            yinYang = "양"
        )

        assertEquals("갑", detail.koreanName)
        assertNull(detail.animal)
    }

    @Test
    fun `test ElementDistribution creation`() {
        val distribution = ElementDistribution(
            count = 5,
            percentage = 50.0
        )

        assertEquals(5, distribution.count)
        assertEquals(50.0, distribution.percentage, 0.001)
    }

    @Test
    fun `test ElementDistribution with zero values`() {
        val distribution = ElementDistribution(count = 0, percentage = 0.0)

        assertEquals(0, distribution.count)
        assertEquals(0.0, distribution.percentage, 0.001)
    }

    // Data class copy tests
    @Test
    fun `test UploadUrlData destructuring`() {
        val data = UploadUrlData("url", "key", "fileId", 3600)
        val (uploadUrl, key, fileId, expiresIn) = data

        assertEquals("url", uploadUrl)
        assertEquals("key", key)
        assertEquals("fileId", fileId)
        assertEquals(3600, expiresIn)
    }

    @Test
    fun `test ImageItem destructuring`() {
        val item = ImageItem("name", "path", "url")
        val (filename, path, url) = item

        assertEquals("name", filename)
        assertEquals("path", path)
        assertEquals("url", url)
    }

    @Test
    fun `test TomorrowGapja destructuring`() {
        val gapja = TomorrowGapja(1, "갑자", "wood")
        val (code, name, element) = gapja

        assertEquals(1, code)
        assertEquals("갑자", name)
        assertEquals("wood", element)
    }

    @Test
    fun `test ElementDistribution destructuring`() {
        val dist = ElementDistribution(3, 30.0)
        val (count, percentage) = dist

        assertEquals(3, count)
        assertEquals(30.0, percentage, 0.001)
    }
}
