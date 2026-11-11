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

- `openBenMemberModal`, `BEN_REL_OPTIONS`, `OTHER_RELATIONS` ← `ui/ben/BenMember.kt` (قفل الاسم/الهوية، الميلاد إلزامي للجديد، حمل/رضاعة للأنثى ≥15، حذف بتأكيدين)
- تبويب «طلباتي» (الإرسال، الإلغاء بتأكيد، التاريخ، عدّاد المعلّق) وشرائح نواقص الملف التي تفتح نافذة الفرد ← `ui/ben/BenHome.kt`
- `NatRef` (list/save/load/remove/_entries/ensure/peek/famOf/hint/suggestFor/fill) ← `data/NatRef.kt` (الكشوف ملفات في filesDir/natrefs) + `NidInput` (تلميح «وارد/غير وارد» وزر تعبئة)
- `detectFamilySummaryColumns`, `detectHorizontalColumns`, `cleanCell`, `normalizeMaritalWord`, `detectSpouseFatePlaceholder`, `resolveGender`, `nameKey` ← `data/NatRef.kt (Cells)`
- `renderNatRefBox`, `natRefAutoSave`, `renderReconcilePreview`, `runReconcileImport`, `findInFamily`, تقرير غير المتطابقين ← `data/NatRef.kt (Reconcile)` + `ui/more/ReconcilePage.kt`
- مركز الجودة: قسم «غير واردة بالكشف الوطني» والهويات المقترحة (`qcApplyNid`، تطبيق الكل) ← `ui/more/QualityPage.kt`

## فروق معروفة عن الويب
- المطابقة الوطنية: `canonicalFamilyId` يستعمل هوية رب الأسرة ثم معرّفه (لا يقرأ حساب المستفيد)، و`FamilyRepair` غير منقول بعد.
- تبويب «إشعاراتي»: «سجل التعديلات على بياناتي» فارغ (لا سجل محلي في Kotlin)، وبطاقة تفعيل الإشعارات نص ثابت.
- صف العناوين في الكشف = الصف الأول من الملف، وتحليل التواريخ عبر `Importer.date` (أبسط من `_parseDateAny`).

## تقريبي (بحاجة مراجعة مقابل الكود)
- الاستيراد (معالج الخطوات، المعاينة، سجل الدفعات والتراجع)
- مركز الجودة (الدمج/إزالة الهوية/التكرار بين المخيمات)، المساعدات، الأسماء المتشابهة، منشئ الاستعلامات
- الرسائل (المحادثة والبث)، سجل التغييرات، معلومات المخيم
