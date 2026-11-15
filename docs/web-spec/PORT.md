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
- الاستيراد: يكمّل الموجودين (بهوية رب الأسرة/الفرد عبر كل المخيمات المحمّلة، الحقول الفارغة فقط، ويضيف الناقصين لأسرهم) ويضيف الجدد.
- ملف النظام: يُصدَّر مع الورقة المخفية `_grcs_data` (سجلات P فقط، بلا صفوف الأسر F)، والاستبعاد من مخيمات أخرى يقتصر على المخيمات المحمّلة على الجهاز (الويب يفحص الفهرس الوطني). الكشف التصنيفي يفحص المخيمات الأخرى عبر الفهرس الوطني عند الاتصال.
- `canonicalFamilyId` لا يقرأ حساب المستفيد؛ `FamilyRepair.auto` غير منقول (يعمل بعد المطابقة فقط).
- بطاقة تفعيل الإشعارات عند المستفيد نص ثابت.
- القوائم المنسدلة كلها ورقة اختيار سفلية (PickSheet) بدل select.

## روجع مقابل الكود (بلا فروق وظيفية مهمة)
- سجل التغييرات: مطابق (بحث للإدارة، 30 يوماً للمندوب، حد 200، حالة «لا نتائج»). معلومات المخيم: مطابقة، وإصلاح R15: تغيير الاسم يُنشر لحسابات المخيم كـpropagateCampName.
- الرسائل: قائمة المندوب (الإدارة + المستفيدون)، قائمة الإدارة (بث عام، بث المستفيدين، صف لكل مخيم بعدّاد)، المحادثة مع دمج الرسائل العامة، صندوق الصادر. الفرق: البث العام يُكتب من شاشة المحادثة لا من نافذة.

## لم يُراجَع سطراً بسطر (الميزات موجودة)
- (لا شيء)

## R15
- الكشف التصنيفي (CatImport.kt + CatImportUi.kt): 13 نوعاً، تحليل أعمدة بالمحتوى، مطابقة بإشارات موزونة، معاينة بأقسام، تقرير، تراجع عبر دفعات الاستيراد. استيراد ملف النظام (Sig) من الورقة المخفية. أزرار بتأثير ضغط واهتزاز.

## R16
- المطابقة الوطنية الشاملة (NatStream.kt): قراءة xlsx/csv بالتدفق (فهرس نصوص مشتركة بهويات الأرباب المسجَّلين ثم مرور واحد بالورقة)، بلا ختم ولا تقرير تعارض؛ تكمّل الحقول الفارغة وتضيف الزوج/ة والأبناء الناقصين بكل مرة. جرّبت على عيّنة 33 ألف صف: ~5 ثوانٍ.
- Cells.phone (0 بادئ) وCells.housing؛ الحالة الصحية ← diseaseDetails، المدينة الأصلية ← customFields، السكن ← livingSituation.
- WorkManager: SyncWorker كل 12 ساعة + kick عند فشل الرفع؛ UpdateWorker يومياً؛ Updater (تنزيل مستأنَف، تحقق حزمة/نسخة/توقيع، FileProvider، إشعار، تثبيت) وUpdateBar.
- DataRepo.sync بقفل واحد للعملية؛ قوائم الأدمن (staff/bens) بذاكرة SQLite تُعرض فوراً؛ MainActivity يستعيد الجلسة محلياً ثم يجددها بالخلفية.
