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

- `openNewUserModal`, `createNewUser`, `ACCOUNT_TYPES` ← `ui/more/StaffPage.kt` + `Staff.create` (نوع الحساب أولاً؛ مندوب بمخيم جديد أو مخيم بلا مندوب)
- `_parseDateAny`, `locateHeaderRow` ← `data/NatRef.kt (Cells.date / headerRow)`؛ `FamilyRepair.scan/apply` ← `data/NatRef.kt (FamilyRepair)` بعد المطابقة
- `mergePersons`, `qcClearNid`، مجموعات الهويات المكررة (دمج بالمختار / إزالة من الباقين / بين المخيمات) ← `data/Merge.persons` + `ui/more/QualityPage.kt`
- سجل تعديلات المستفيد (changelog المحلي) ← `DataRepo.benLog`

## إضافات بطلب ساجد (ليست في الويب)
- إنشاء حساب «مستفيد» يدوياً، ومدخل بيانات/مستفيد بلا مخيم (يُربط لاحقاً من «تعديل»). حساب طاقم بلا مخيم يرى شاشة «غير مرتبط بمخيم».
- الضغط على مستفيد في الحسابات يفتح سجله. أزرار تفاصيل الفرد شبكة 2×2. حدّ تكبير خط النظام 1.1.

## فروق معروفة عن الويب
- `canonicalFamilyId` لا يقرأ حساب المستفيد؛ `FamilyRepair.auto` (التشغيل التلقائي عند تغيّر البيانات) غير منقول — يعمل بعد المطابقة فقط.
- بطاقة تفعيل الإشعارات عند المستفيد نص ثابت. استيراد المخيم ما زال يقرأ الصف الأول كعناوين.

## تقريبي (بحاجة مراجعة مقابل الكود)
- الاستيراد (معالج الخطوات، المعاينة، سجل الدفعات والتراجع)
- المساعدات، الأسماء المتشابهة، منشئ الاستعلامات
- الرسائل (المحادثة والبث)، سجل التغييرات، معلومات المخيم
