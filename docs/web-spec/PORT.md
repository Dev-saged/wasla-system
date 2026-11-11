# سجل النقل من index.html إلى Kotlin

كل سطر: دالة الويب ← مكانها في Kotlin، وحالتها. «مطابق» = نُقل المنطق سطراً بسطر. «تقريبي» = يعمل لكنه لم يُراجع مقابل الكود بعد.

## مطابق
- `Derive.keys / has / ageBucket` ← `data/Derive.kt` (مصدر واحد للبطاقات والفلاتر؛ «بلا هاتف» لأرباب الأسر فقط)
- `computeStats` ← `data/Stats.compute` (يقرأ من Derive)
- `PERSON_FILTER_LABELS` وشرائح الأفراد ← `data/PFilter.kt`
- `relationLabel`, `CHILD_RELATIONS`, `childGenderFromRel` ← `data/Rel.kt` (relation = head/wife/husband/child/orphan/other + memberRelation)
- `normNid`, `isValidPalId`, `nidStatus` ← `data/Rel.kt (Nid)` + `data/Quality.nidStatus`
- `NidField.validate / refresh` ← `ui/people/PersonForm.kt` (NidState, NidInput)
- `openPersonForm` + حفظ النموذج (رب الأسرة + الزوج/ة + أفراد الأسرة + الحذف) ← `ui/people/PersonForm.kt` + `DataRepo.saveForm`
- `Rapid*` (rapidSetRel, rapidChain, rapidFinalName, rapidSave, rapidUndo) ← `ui/more/RapidPage.kt`
- `handleStatCardClick`, `adminViewingCampId`, `updateFabVisibility`, `renderBottomNav` ← `ui/shell/AppShell.kt` (ShellState.adminCamp)
- `openGlobalSearch`, `gsActions` ← `ui/shell/GlobalSearch.kt`
- `renderAdminCampActions` ← `ui/home/HomeScreen.kt`
- `refreshSettingsScreen` ← `ui/more/MoreScreen.kt`
- `refreshAccountsScreen` (البطاقات) ← `ui/more/StaffPage.kt`
- `openPersonDetail`, `personCardHtml`, `openTagsEditor` ← `ui/people/*`

## تقريبي (بحاجة مراجعة مقابل الكود)
- نافذة فرد المستفيد `openBenMemberForm` (الويب 14150+) — يستعمل حالياً نموذج الفرد العام
- المطابقة مع الكشف الوطني `NatRef` / `reconcile` (تخزين الكشف، المطابقة، التتبع)
- الاستيراد (معالج الخطوات، المعاينة، سجل الدفعات والتراجع)
- مركز الجودة، المساعدات، الأسماء المتشابهة، منشئ الاستعلامات
- الرسائل (المحادثة والبث)، سجل التغييرات، معلومات المخيم
- تبويبات المستفيد: طلباتي، إشعاراتي
