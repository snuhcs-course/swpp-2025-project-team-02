#!/usr/bin/env python
"""
닉네임 중복 허용 수동 테스트 스크립트
"""
import os
import sys
import django
from datetime import date

# Django 설정
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'fortuna_api.settings.testing')
django.setup()

from django.contrib.auth import get_user_model

User = get_user_model()

def test_nickname_duplication():
    """닉네임 중복 테스트"""
    print("=" * 60)
    print("닉네임 중복 허용 테스트 시작")
    print("=" * 60)

    # 기존 테스트 사용자 삭제
    User.objects.filter(email__startswith='nickname_test').delete()
    print("\n✓ 기존 테스트 데이터 정리 완료")

    # 테스트 1: 동일한 닉네임으로 두 사용자 생성
    print("\n[테스트 1] 동일한 닉네임으로 두 사용자 생성")
    try:
        user1 = User.objects.create_user(
            email='nickname_test1@example.com',
            first_name='Test User 1',
            nickname='duplicateNick',
            birth_date_solar=date(1990, 1, 15),
            birth_time_units='묘시',
            gender='M',
            solar_or_lunar='solar'
        )
        print(f"  사용자 1 생성 성공: {user1.email}, 닉네임: {user1.nickname}")

        user2 = User.objects.create_user(
            email='nickname_test2@example.com',
            first_name='Test User 2',
            nickname='duplicateNick',  # 동일한 닉네임
            birth_date_solar=date(1991, 2, 20),
            birth_time_units='오시',
            gender='F',
            solar_or_lunar='solar'
        )
        print(f"  사용자 2 생성 성공: {user2.email}, 닉네임: {user2.nickname}")
        print("  ✓ 테스트 1 통과: 중복 닉네임으로 사용자 생성 가능")
    except Exception as e:
        print(f"  ✗ 테스트 1 실패: {str(e)}")
        return False

    # 테스트 2: 닉네임으로 조회 시 여러 사용자 반환
    print("\n[테스트 2] 동일한 닉네임을 가진 사용자 조회")
    try:
        users_with_same_nickname = User.objects.filter(nickname='duplicateNick')
        count = users_with_same_nickname.count()
        print(f"  'duplicateNick' 닉네임을 가진 사용자 수: {count}")

        if count == 2:
            print("  ✓ 테스트 2 통과: 동일한 닉네임을 가진 여러 사용자 조회 성공")
        else:
            print(f"  ✗ 테스트 2 실패: 예상 2명, 실제 {count}명")
            return False
    except Exception as e:
        print(f"  ✗ 테스트 2 실패: {str(e)}")
        return False

    # 테스트 3: 기존 사용자의 닉네임을 다른 사용자와 동일하게 변경
    print("\n[테스트 3] 기존 사용자의 닉네임을 중복된 값으로 변경")
    try:
        user3 = User.objects.create_user(
            email='nickname_test3@example.com',
            first_name='Test User 3',
            nickname='uniqueNick',
            birth_date_solar=date(1992, 3, 25),
            birth_time_units='신시',
            gender='M',
            solar_or_lunar='solar'
        )
        print(f"  사용자 3 생성 성공: {user3.email}, 닉네임: {user3.nickname}")

        # 닉네임을 중복된 값으로 변경
        user3.nickname = 'duplicateNick'
        user3.save()
        print(f"  닉네임 변경 성공: {user3.email}, 새 닉네임: {user3.nickname}")

        # 변경 확인
        user3.refresh_from_db()
        if user3.nickname == 'duplicateNick':
            print("  ✓ 테스트 3 통과: 닉네임을 중복된 값으로 변경 가능")
        else:
            print(f"  ✗ 테스트 3 실패: 닉네임이 변경되지 않음")
            return False
    except Exception as e:
        print(f"  ✗ 테스트 3 실패: {str(e)}")
        return False

    # 테스트 4: 최종 통계
    print("\n[테스트 4] 최종 통계 확인")
    try:
        total_users = User.objects.filter(email__startswith='nickname_test').count()
        duplicate_nick_users = User.objects.filter(nickname='duplicateNick').count()

        print(f"  총 테스트 사용자 수: {total_users}")
        print(f"  'duplicateNick' 닉네임 사용자 수: {duplicate_nick_users}")

        if duplicate_nick_users == 3:
            print("  ✓ 테스트 4 통과: 3명의 사용자가 동일한 닉네임 사용 중")
        else:
            print(f"  ✗ 테스트 4 실패: 예상 3명, 실제 {duplicate_nick_users}명")
            return False
    except Exception as e:
        print(f"  ✗ 테스트 4 실패: {str(e)}")
        return False

    # 정리
    print("\n[정리] 테스트 데이터 삭제")
    User.objects.filter(email__startswith='nickname_test').delete()
    print("  ✓ 테스트 데이터 삭제 완료")

    print("\n" + "=" * 60)
    print("모든 테스트 통과!")
    print("=" * 60)
    return True

if __name__ == '__main__':
    try:
        success = test_nickname_duplication()
        sys.exit(0 if success else 1)
    except Exception as e:
        print(f"\n예상치 못한 오류 발생: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
