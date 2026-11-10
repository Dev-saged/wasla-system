"""بيانات تجريبية ثابتة تُستعمل في تصوير الويب وKotlin معًا."""
import json, sys, datetime
T = datetime.date(2026, 11, 10)
def ago(y, d=0):
    return (T.replace(year=T.year - y) - datetime.timedelta(days=d)).isoformat()
NOW = 1794300000000
camps = [{"id": "camp1", "name": "مخيم الأمل", "location": "خانيونس - المواصي"}, {"id": "camp2", "name": "مخيم الصمود", "location": "دير البلح"}]
P = []
def fam(camp, fid, head, members, **hx):
    def mk(i, name, rel, g, age, **x):
        nid = x.pop("nid", "4%08d" % (len(P) * 7919 % 99999999))
        P.append({"id": f"{fid}-{i}", "campId": camp, "familyId": fid, "fullName": name, "nationalId": nid, "gender": g,
                  "birthDate": ago(*age) if isinstance(age, tuple) else ago(age, 40), "relation": rel, "isFamilyHead": rel == "head",
                  "orderIndex": i, "phone": "", "maritalStatus": "married" if rel in ("head", "wife") else "single",
                  "isPregnant": False, "isBreastfeeding": False, "hasChronicDisease": False, "chronicConditions": [], "hasDisability": False,
                  "address": "", "createdAt": NOW - 86400000 * (30 - len(P) % 20), "updatedAt": NOW - 3600000 * (len(P) % 50), **x})
    mk(0, *head, **hx)
    for i, m in enumerate(members, 1):
        x = m[4] if len(m) > 4 else {}
        mk(i, m[0], m[1], m[2], m[3], **x)
fam("camp1", "f01", ("محمد أحمد سالم النجار", "head", "male", 44), [("سعاد خليل النجار", "wife", "female", 39, {"isPregnant": True}), ("أحمد محمد النجار", "son", "male", 15), ("ليان محمد النجار", "daughter", "female", 9), ("يوسف محمد النجار", "son", "male", (0, 120))], phone="0599123456", address="خيمة 14 - بلوك ب", shelterType="tent")
fam("camp1", "f02", ("فاطمة يوسف حسن أبو عودة", "head", "female", 67), [("خالد سامي أبو عودة", "son", "male", 31, {"hasDisability": True, "disabilityDetails": "حركية"})], phone="0567001122", maritalStatus="widowed", hasChronicDisease=True, chronicConditions=["diabetes", "hypertension"])
fam("camp1", "f03", ("إبراهيم سليم عبد الله الأسطل", "head", "male", 52), [("منى عادل الأسطل", "wife", "female", 45, {"hasChronicDisease": True, "chronicConditions": ["respiratory"]}), ("سليم إبراهيم الأسطل", "son", "male", 19), ("ريم إبراهيم الأسطل", "daughter", "female", 13), ("نور إبراهيم الأسطل", "daughter", "female", 6), ("آدم إبراهيم الأسطل", "son", "male", (1, 60))], phone="0598765432")
fam("camp1", "f04", ("عبير ماهر شعت", "head", "female", 29), [("ملك وائل شعت", "daughter", "female", 4), ("كريم وائل شعت", "son", "male", (0, 200), {"nid": ""})], maritalStatus="divorced", isBreastfeeding=True)
fam("camp1", "f05", ("حسن علي مصطفى البيوك", "head", "male", 73), [("آمنة حسين البيوك", "wife", "female", 66)], nid="", hasChronicDisease=True, chronicConditions=["heart"])
fam("camp1", "f06", ("سامي نبيل زعرب", "head", "male", 35), [("هبة فؤاد زعرب", "wife", "female", 30, {"isBreastfeeding": True}), ("نبيل سامي زعرب", "son", "male", 7), ("جنى سامي زعرب", "daughter", "female", (1, 300))], phone="0592223344")
fam("camp2", "g01", ("رامي جمال الفرا", "head", "male", 41), [("دعاء سمير الفرا", "wife", "female", 36), ("جمال رامي الفرا", "son", "male", 12)], phone="0599000111")
fam("camp2", "g02", ("نعيمة صالح بربخ", "head", "female", 70), [], maritalStatus="widowed", hasDisability=True)
fam("camp2", "g03", ("زياد عمر قشطة", "head", "male", 27), [("سلمى هاني قشطة", "wife", "female", 24, {"isPregnant": True})], phone="0569111222")
users = {r: {"id": "u-" + r, "uid": "u-" + r, "username": r, "displayName": n, "name": n, "role": r, "campId": c, "campName": cn, "active": True, "isActive": True}
         for r, n, c, cn in [("admin", "ساجد العبادلة", "", ""), ("delegate", "أبو خالد المندوب", "camp1", "مخيم الأمل"), ("dataentry", "مدخل البيانات", "camp1", "مخيم الأمل")]}
users["beneficiary"] = {"id": "u-ben", "uid": "u-ben", "username": P[0]["nationalId"], "displayName": P[0]["fullName"], "name": P[0]["fullName"], "role": "beneficiary", "campId": "camp1", "campName": "مخيم الأمل", "personId": P[0]["id"], "familyId": "f01", "active": True, "isActive": True}
json.dump({"camps": camps, "persons": P, "users": users, "now": NOW}, open(sys.argv[1], "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print(len(P), "persons")
