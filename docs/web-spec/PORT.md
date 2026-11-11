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

- `ImportBatch` (commit/latest/canUndo/undo/diff), `runUndoImport`, `openImportBatches`, `showBatchDiff`, `rowSig` ← `data/ImportBatch.kt` + `ui/more/ImportPage.kt`
- `findSimilarGroups` + بطاقة الدمج بالمختار / «ليسا نفس الشخص» ← `ui/more/SimilarPage.kt`
- المساعدات: `aidRowsFor`, `aidAskAmount`, التسليم الجماعي `aid-bulk`, كشف التوزيع `aid-export`, الفلتر الافتراضي «لم يستلموا» ← `ui/more/AidScreen.kt`
- منشئ الاستعلامات: `QB_FIELDS` (مع المخيم والمرض المزمن وهوية غير صحيحة), `qbTest`, `qbRender`, `qbRenderPresets`, `qbSave` ← `data/Query.kt` + `ui/more/QueryPage.kt`
- `ACTION_LABELS` كاملة ← `data/Audit.labels`

## إضافات بطلب ساجد (ليست في الويب)
- إنشاء حساب «مستفيد» يدوياً، ومدخل بيانات/مستفيد بلا مخيم (يُربط لاحقاً من «تعديل»). حساب طاقم بلا مخيم يرى شاشة «غير مرتبط بمخيم».
- الضغط على مستفيد في الحسابات يفتح سجله. أزرار تفاصيل الفرد شبكة 2×2. حدّ تكبير خط النظام 1.1.

## فروق معروفة عن الويب
- الاستيراد يضيف الجدد فقط (لا يحدّث الموجودين كما في `runImport*` بالويب)، فلقطة `pre` في الدفعة فارغة؛ أوضاع «أسرة مختصرة» و«كشف تصنيفي» و«ملف النظام» في الاستيراد غير منقولة.
- تصدير منشئ الاستعلامات يستعمل أعمدة التصدير العام لا أعمدة `qbExport` (ولي الأمر/الهشاشة).
- `canonicalFamilyId` لا يقرأ حساب المستفيد؛ `FamilyRepair.auto` غير منقول (يعمل بعد المطابقة فقط).
- بطاقة تفعيل الإشعارات عند المستفيد نص ثابت.

## لم يُراجَع سطراً بسطر (الميزات موجودة)
- الرسائل: القائمة، المحادثة، البث العام، محادثات المستفيدين، بث المخيم وبث كل المستفيدين، الشارة.
- سجل التغييرات (بحث + تصدير)، معلومات المخيم.
