"""
Traditional Korean Saju (사주) system with 60 Gapja codes.
This module contains the complete system of Heavenly Stems (천간) and Earthly Branches (지지).
"""

# 십간 (Ten Heavenly Stems)
HEAVENLY_STEMS = [
    {"name": "갑", "element": "목", "yin_yang": "양", "chinese": "甲"},
    {"name": "을", "element": "목", "yin_yang": "음", "chinese": "乙"},
    {"name": "병", "element": "화", "yin_yang": "양", "chinese": "丙"},
    {"name": "정", "element": "화", "yin_yang": "음", "chinese": "丁"},
    {"name": "무", "element": "토", "yin_yang": "양", "chinese": "戊"},
    {"name": "기", "element": "토", "yin_yang": "음", "chinese": "己"},
    {"name": "경", "element": "금", "yin_yang": "양", "chinese": "庚"},
    {"name": "신", "element": "금", "yin_yang": "음", "chinese": "辛"},
    {"name": "임", "element": "수", "yin_yang": "양", "chinese": "壬"},
    {"name": "계", "element": "수", "yin_yang": "음", "chinese": "癸"},
]

# 십이지 (Twelve Earthly Branches)
EARTHLY_BRANCHES = [
    {"name": "자", "animal": "쥐", "element": "수", "chinese": "子"},
    {"name": "축", "animal": "소", "element": "토", "chinese": "丑"},
    {"name": "인", "animal": "호랑이", "element": "목", "chinese": "寅"},
    {"name": "묘", "animal": "토끼", "element": "목", "chinese": "卯"},
    {"name": "진", "animal": "용", "element": "토", "chinese": "辰"},
    {"name": "사", "animal": "뱀", "element": "화", "chinese": "巳"},
    {"name": "오", "animal": "말", "element": "화", "chinese": "午"},
    {"name": "미", "animal": "양", "element": "토", "chinese": "未"},
    {"name": "신", "animal": "원숭이", "element": "금", "chinese": "申"},
    {"name": "유", "animal": "닭", "element": "금", "chinese": "酉"},
    {"name": "술", "animal": "개", "element": "토", "chinese": "戌"},
    {"name": "해", "animal": "돼지", "element": "수", "chinese": "亥"},
]

# 60갑자 (60 Gapja combinations)
def generate_gapja_combinations():
    """Generate all 60 Gapja combinations."""
    combinations = []
    for i in range(60):
        stem_index = i % 10
        branch_index = i % 12
        stem = HEAVENLY_STEMS[stem_index]
        branch = EARTHLY_BRANCHES[branch_index]

        combinations.append({
            "code": i + 1,
            "korean_name": stem["name"] + branch["name"],
            "chinese_characters": stem["chinese"] + branch["chinese"],
            "heavenly_stem": stem["name"],
            "earthly_branch": branch["name"],
            "stem_element": stem["element"],
            "branch_element": branch["element"],
            "yin_yang": stem["yin_yang"],
            "animal": branch["animal"],
        })

    return combinations

# Complete 60 Gapja system
GAPJA_SYSTEM = generate_gapja_combinations()

# Five Elements (오행) interactions
FIVE_ELEMENTS = {
    "목": {"name": "Wood", "korean": "목", "color": ["green", "brown"], "direction": "동"},
    "화": {"name": "Fire", "korean": "화", "color": ["red", "orange"], "direction": "남"},
    "토": {"name": "Earth", "korean": "토", "color": ["yellow", "beige"], "direction": "중앙"},
    "금": {"name": "Metal", "korean": "금", "color": ["white", "gold"], "direction": "서"},
    "수": {"name": "Water", "korean": "수", "color": ["blue", "black"], "direction": "북"},
}

# Element generation cycle (상생)
GENERATION_CYCLE = {
    "목": "화",  # Wood feeds Fire
    "화": "토",  # Fire creates Earth (ash)
    "토": "금",  # Earth contains Metal
    "금": "수",  # Metal collects Water
    "수": "목",  # Water nourishes Wood
}

# Element destruction cycle (상극)
DESTRUCTION_CYCLE = {
    "목": "토",  # Wood depletes Earth
    "화": "금",  # Fire melts Metal
    "토": "수",  # Earth absorbs Water
    "금": "목",  # Metal cuts Wood
    "수": "화",  # Water extinguishes Fire
}

def get_gapja_by_code(code_number):
    """Get Gapja information by code number (1-60)."""
    if 1 <= code_number <= 60:
        return GAPJA_SYSTEM[code_number - 1]
    return None

def get_element_compatibility(element1, element2):
    """Check compatibility between two elements."""
    if GENERATION_CYCLE.get(element1) == element2:
        return "generates"
    elif GENERATION_CYCLE.get(element2) == element1:
        return "is_generated_by"
    elif DESTRUCTION_CYCLE.get(element1) == element2:
        return "destroys"
    elif DESTRUCTION_CYCLE.get(element2) == element1:
        return "is_destroyed_by"
    elif element1 == element2:
        return "same"
    else:
        return "neutral"

def calculate_compatibility_score(code1, code2):
    """Calculate compatibility score between two Gapja codes."""
    gapja1 = get_gapja_by_code(code1)
    gapja2 = get_gapja_by_code(code2)

    if not gapja1 or not gapja2:
        return 0

    stem_compat = get_element_compatibility(gapja1["stem_element"], gapja2["stem_element"])
    branch_compat = get_element_compatibility(gapja1["branch_element"], gapja2["branch_element"])

    score_map = {
        "generates": 80,
        "is_generated_by": 80,
        "same": 70,
        "neutral": 50,
        "destroys": 20,
        "is_destroyed_by": 20,
    }

    stem_score = score_map.get(stem_compat, 50)
    branch_score = score_map.get(branch_compat, 50)

    return (stem_score + branch_score) // 2