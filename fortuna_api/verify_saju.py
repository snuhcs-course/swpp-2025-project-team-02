"""
양력/음력 변환 및 사주 계산 검증 스크립트

양력 2000년 3월 17일과 음력 2000년 2월 12일이 동일한 날인지,
그리고 각각의 사주 계산 결과를 확인합니다.
"""

from datetime import date, time
from core.utils.saju_concepts import SajuCalculator

# 양력 2000년 3월 17일
solar_date = date(2000, 3, 17)
print(f"양력 입력: {solar_date}")

# 양력 -> 음력 변환
converted_lunar = SajuCalculator.solar_to_lunar(solar_date)
print(f"양력 -> 음력 변환: {converted_lunar}")
print()

# 음력 2000년 2월 12일
lunar_date = date(2000, 2, 12)
print(f"음력 입력: {lunar_date}")

# 음력 -> 양력 변환
converted_solar = SajuCalculator.lunar_to_solar(lunar_date)
print(f"음력 -> 양력 변환: {converted_solar}")
print()

# 같은 날인지 확인
print("=" * 50)
print(f"변환 검증:")
print(f"  양력 {solar_date} -> 음력 {converted_lunar}")
print(f"  음력 {lunar_date} -> 양력 {converted_solar}")
print(f"  동일한 날? {solar_date == converted_solar and lunar_date == converted_lunar}")
print()

# 사주 계산 (미시 = 14:00)
birth_time = time(14, 0)
print("=" * 50)
print(f"사주 계산 (미시 14:00 기준)")
print()

# 양력 날짜로 사주 계산
saju_from_solar = SajuCalculator.calculate_saju(solar_date, birth_time)
print(f"양력 {solar_date} 사주:")
print(f"  년주: {saju_from_solar['yearly_ganji']}")
print(f"  월주: {saju_from_solar['monthly_ganji']}")
print(f"  일주: {saju_from_solar['daily_ganji']}")
print(f"  시주: {saju_from_solar['hourly_ganji']}")
print()

# 변환된 양력 날짜로 사주 계산 (음력->양력 변환 후)
saju_from_converted_solar = SajuCalculator.calculate_saju(converted_solar, birth_time)
print(f"음력 {lunar_date} -> 양력 {converted_solar} 사주:")
print(f"  년주: {saju_from_converted_solar['yearly_ganji']}")
print(f"  월주: {saju_from_converted_solar['monthly_ganji']}")
print(f"  일주: {saju_from_converted_solar['daily_ganji']}")
print(f"  시주: {saju_from_converted_solar['hourly_ganji']}")
print()

# 결과 비교
print("=" * 50)
print(f"사주 계산 결과 비교:")
print(f"  년주 일치: {saju_from_solar['yearly_ganji'] == saju_from_converted_solar['yearly_ganji']}")
print(f"  월주 일치: {saju_from_solar['monthly_ganji'] == saju_from_converted_solar['monthly_ganji']}")
print(f"  일주 일치: {saju_from_solar['daily_ganji'] == saju_from_converted_solar['daily_ganji']}")
print(f"  시주 일치: {saju_from_solar['hourly_ganji'] == saju_from_converted_solar['hourly_ganji']}")
print()
print(f"✅ 모든 사주가 일치: {saju_from_solar == saju_from_converted_solar}")
