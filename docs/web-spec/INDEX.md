# Screens (markup line ranges)

- screen-splash: lines 1044-1054; ids=2
- screen-login: lines 1055-1090; ids=10
- screen-dash: lines 1091-1114; ids=16
- screen-category: lines 1115-1127; ids=6
- screen-persons: lines 1128-1178; ids=11
- screen-person-form: lines 1179-1289; ids=43
- screen-import: lines 1290-1349; ids=34
- screen-export: lines 1350-1405; ids=22
- screen-messages-list: lines 1406-1414; ids=2
- screen-chat: lines 1415-1430; ids=7
- screen-ben-threads: lines 1431-1442; ids=5
- screen-camp-info: lines 1443-1484; ids=15
- screen-settings: lines 1485-1493; ids=2
- screen-audit: lines 1494-1503; ids=4
- screen-camps-admin: lines 1504-1520; ids=9
- screen-accounts-admin: lines 1521-1541; ids=5
- screen-beneficiary: lines 1542-1626; ids=41
- screen-rapid: lines 1627-1665; ids=21
- screen-quality: lines 1666-1673; ids=4
- screen-aid: lines 1674-1692; ids=12
- screen-similar: lines 1693-1704; ids=4
- screen-query: lines 1705-1725; ids=11

# Functions (name:line)

uuid:1766, esc:1772, randomSaltB64:1776, pbkdf2Hash:1780, sha256Hex:1788, pwVerifier:1792, hasPassword:1798, applyPassword:1800, matchPassword:1808, gzipCompressToB64:1816, gzipDecompressFromB64:1823, fsBaseUrl:1833, fsKeyParam:1834, fsEncodeValue:1835, fsEncodeFields:1844, fsDecodeValue:1849, fsDecodeFields:1861, fetchWithTimeout:1869, _fetchOnce:1894, withTimeout:1909, fsDocGet:1915, fsDocSet:1924, fsDocPatch:1939, fsDocDelete:1947, fsListDocuments:1953, fsRunQuery:1976, fsDocName:2026, fsCommitRaw:2029, fsBatchWrite:2038, fsBatchGet:2071, stampNow:2116, fsQueryUserByUsername:2238, calcAge:2250, ageCategory:2260, ageBucket:2268, isWidowed:2332, isDivorced:2333, fmtDate:2334, debounce:2341, yn:2344, formatChronicDetails:2353, bytesToB64:2360, b64ToBytes:2365, toLatinDigits:2379, normNid:2383, isValidPalId:2388, nidStatus:2396, renderNatRefBox:2606, natRefAutoSave:2642, _ymd:2660, _expandYear:2668, _excelSerial:2669, _parseDateAny:2674, nameKey:2704, nameTokens:2707, jaroWinkler:2708, levenshtein:2728, dupScore:2742, famKey:2782, buildPersonIndex:2783, _bumpPersons:2808, campVersion:2812, statsOf:2889, reducedMotion:2959, countUpIn:2962, ringHtml:3049, ringsIn:3057, renderBatched:3068, syncHealStale:3221, syncRunAndReport:3227, openSyncDiag:3240, toast:3307, openModal:3330, closeModal:3350, confirmDialog:3360, promptDialog:3378, dbOpen:3429, dbGet:3455, dbGetAll:3462, dbIndexAll:3469, _bumpStore:3478, _campsOf:3479, storeVersion:3480, dbPut:3481, dbDelete:3489, dbCount:3499, dbClear:3506, dbBulkPut:3515, nav:3539, _navCore:3547, decodeJwtPayload:3653, adoptServerUser:3661, normUsername:3676, findUserByUsername:3680, userErr:3688, login:3690, forceLogout:3740, logout:3748, restoreSession:3762, guardRole:3788, renderBottomNav:3816, updateFabVisibility:3839, openFirstPasswordSetupModal:3848, resolveLoginUser:3905, pwRevealHtml:3953, bindPwReveal:3959, findBenUserForPerson:3978, loginInfoHtml:3984, needsForcedPwChange:3995, commitOwnPasswordChange:4002, openForcedPasswordModal:4013, maybeForcePasswordChange:4042, enterApp:4045, roleLabel:4079, roleHeaderText:4080, relationLabel:4085, currentCampId:4099, getAge:4100, statCard:4102, statCardCustom:4109, emptyStateHtml:4113, flashConnBadge:4118, renderConnBadge:4125, setHTML:4133, objId:4140, idleRender:4144, memoRender:4153, renderOfflineBanner:4163, renderPendingSyncBanner:4168, familyCountOf:4181, campFamilyCount:4183, loadPersonsCache:4184, computeStats:4189, shelterLabel:4213, inferShelterFromText:4215, effectiveShelter:4227, phone2Of:4236, famCount:4250, tierLabel:4271, tierChipHtml:4331, vulnCardHtml:4336, qualityRuleApplies:4364, qualityTest:4365, qualityReport:4370, dupNidsOf:4386, dobSuspect:4394, qualityAnalyze:4411, completeness:4438, qcPersonLine:4440, qcEdit:4445, qcApplyNid:4451, qcClearNid:4462, qcSection:4471, aidFamHead:4638, aidRowsFor:4639, aidSummaryHtml:4650, renderAidList:4654, aidAskAmount:4689, aidReplace:4703, renderAidAdminSummary:4704, aidDetailHtml:4787, aidAddDialog:4794, todayStr:4810, buildSnapshot:4815, renderTrendChart:4873, renderTrendsSection:4904, pyramidCounts:4924, renderPyramid:4934, openFilteredList:4962, refreshCampDashboard:4968, renderAdminCampActions:4995, refreshDataEntryDashboard:5032, evenCards:5057, renderDashStats:5062, renderDashCharts:5088, renderTierBar:5104, renderQualityTasks:5119, handleStatCardClick:5146, updateAdminBackBtns:5174, matchesPersonFilter:5191, filterLabel:5204, getFilteredPersons:5214, findFamilyHead:5224, guardianCols:5229, downloadCurrentPersonsFilterExcel:5234, personCardHtml:5283, attachPersonCardEvents:5314, setupVirtualList:5327, renderPersonsList:5354, criticalBadgesRow:5408, getFamilyMembers:5425, auditValueText:5436, summarizeAuditChange:5445, buildPersonDetailRows:5454, openPersonDetail:5489, deletePerson:5587, renderChronicGrid:5627, updateMaternalVisibility:5655, updateSpouseMaternalVisibility:5666, _updateSpouseCardVisibility:5703, childRelLabel:5719, childGenderFromRel:5720, childDvHtml:5721, renderChildRows:5724, pfChildDv:5750, pfRefreshDv:5775, openPersonForm:5784, ensureFamily:5891, normalizeArabic:6048, headerMatchesAny:6056, matchMetaKey:6070, extractCampMetadata:6075, unmergeRows:6109, locateDataSheet:6122, locateHeaderRow:6140, detectRepeatingChildGroups:6155, classifyImportShape:6171, detectAgeBracketLabel:6191, detectFamilySummaryColumns:6203, matchHeaders:6289, pad2:6304, parseBoolCell:6305, parseGenderCell:6309, parseDateCell:6315, cleanCell:6320, normalizeMaritalWord:6334, detectSpouseFatePlaceholder:6356, isSequenceColumn:6369, inferGenderFromName:6451, inferGenderForPerson:6472, resolveGender:6510, dedupAgainstOtherCamps:6518, saveExcludedDuplicatesReport:6553, detectHorizontalColumns:6563, interpretHealthStatus:6595, renderHorizontalPreview:6604, ensureBeneficiaryUser:6623, syncLinkedBeneficiaryUser:6652, userForServer:6708, pushUsersBatch:6721, renderCampMetaPanel:6741, applyCampMetaToCurrentCamp:6753, renderFamilySummaryPreview:6763, runImportFamilySummary:6784, mergeImportRec:6965, importClash:6977, renderReconcilePreview:6981, saveReconcileUnmatchedReport:7004, canonicalFamilyId:7013, findInFamily:7089, runReconcileImport:7100, runImportHorizontal:7265, getSheetJSSource:7434, ensureSheetJS:7445, scheduleLibPrefetch:7491, setImportStage:7496, readFileBuffer:7506, decodeCsvBuffer:7516, isoDateCells:7522, parseWorkbookSync:7542, parseWorkbookWorker:7554, readWorkbook:7577, resetImportScreen:7592, fnv1a:7604, headersFingerprint:7605, headerConfidence:7625, applyFlatTemplate:7635, renderMatchStep:7647, renderRoleWizard:7693, renderHorizontalPreviewTable:7719, renderFamilySummaryPreviewTable:7724, _dv:7733, recordChanges:7734, rowSig:7739, showUndoOnDone:7813, runUndoImport:7818, openImportBatches:7832, showBatchDiff:7862, tagLabel:7895, orphanInfo:7897, isOrphan:7898, phoneKey:7904, parseOrphanType:7905, catProfile:7912, analyzeCatColumns:7930, detectCatFromTable:7977, catRowValues:8004, buildCatIndex:8021, catFamilyHead:8035, catFindSubject:8039, catFindFamily:8058, catNeedsChange:8090, applyCatEffect:8092, buildCatPlan:8147, applyCatPlan:8214, saveCatReport:8275, workbookTable:8285, catScopeCampId:8299, openCatPicker:8300, sanitizeTagLabel:8327, startCatImportFromFile:8328, openCatAnalysis:8338, downloadOtherCampReport:8398, showCatResult:8416, tagBadgesHtml:8443, catExtraCols:8453, openCategoriesPanel:8484, exportCategoryExcel:8514, openTagsEditor:8532, handleImportFile:8596, runImport:8665, populateExportFamilyDropdown:8859, getExportFilters:8863, applyExportFilters:8879, getExportMode:8898, groupPersonsIntoFamilies:8908, getFilteredFamilies:8927, updateExportPreview:8936, buildIndividualExportRows:8951, buildFamilyExportRows:8966, injectScriptCode:8996, loadExcelJS:8999, xlThinBorder:9021, timestampSlug:9025, sanitizeSheetName:9031, buildLegacyExcelBytes:9035, buildStyledExcelBytes:9051, saveExportLocally:9126, triggerBlobDownload:9133, redownloadSavedExport:9139, renderSavedExports:9145, exportToExcel:9157, rowsToCsv:9198, exportToCsv:9201, printReport:9217, formHKey:9279, formRelOf:9280, detectFormSheet:9285, formYes:9301, formNoId:9302, buildGrcsFormBytes:9305, downloadGrcsForm:9379, formParse:9394, formPlan:9406, formRecOf:9483, formEsc:9509, runFormImport:9510, formReport:9548, runFormImportExec:9557, buildGrcsSignatureSheet:9661, readGrcsSignature:9669, runNativeImport:9683, downloadCampFullExcel:9768, destroyChart:9804, renderGenderPie:9806, arc:9813, renderAgeBar:9837, renderCampsBarChart:9865, renderCriticalBarChart:9897, isBenThread:9932, stopChatPolling:9933, startChatPolling:9934, pullRelevantMessages:9955, lastReadKey:9962, getLastRead:9963, markThreadRead:9965, threadListItemHtml:9973, refreshMessagesList:9980, openChat:10035, renderChatMsgs:10050, sendChatMessage:10072, openBroadcastComposer:10091, pushBenMessageNow:10111, pullBenMessagesForCamp:10120, openBeneficiarySupportChat:10133, openBenThreadsScreen:10138, sendBeneficiaryBroadcast:10179, openBeneficiaryBroadcastComposer:10204, sendAllBeneficiariesBroadcast:10231, openAllBeneficiariesBroadcastComposer:10256, updateMessagesBadge:10281, getDirtyCamps:10336, markCampDirty:10339, clearCampDirty:10344, addTombstone:10347, mergeRequests:10359, getCampClearedAt:10368, setCampClearedAt:10369, _pTime:10370, getTombstones:10371, pushNationalIndexEntry:10376, removeNationalIndexEntry:10385, pushNationalIndexBatch:10389, checkNationalIndex:10402, famDocId:10413, personDocId:10417, stripLocal:10418, safeJson:10419, isPermErr:10420, benFamIds:10422, decodeFamDoc:10428, fsFamQuery:10434, dbPutPersonsGuarded:10448, pushCampPersonsBlob:10767, pullCampPersonsBlob:10768, flagPersonConflict:10770, getPersonConflict:10778, clearPersonConflict:10786, syncCampPersonsSoon:10794, pushDirtyCamps:10801, pullAllCampsForAdmin:10806, pullActivityLogs:10818, maybeAdminPull:10829, pushCampInfo:10838, syncCampInfoNow:10842, saveCampInfo:10851, propagateCampName:10863, pullCampsFromServer:10874, pullOwnCampInfo:10895, fetchScopedRemoteUsers:10910, putUserSafe:10921, syncUsersFromFirestore:10930, pushUserDirect:10979, syncUserNow:10986, pushMessageNow:10997, pullThreadMessages:11006, fsQueryByServerTime:11012, pullMsgColl:11033, pullCampMessagesAll:11057, pullChangedMessages:11064, queueSyncOp:11074, processSyncQueue:11077, manualSyncNow:11144, dbPruneByIndex:11159, runGarbageCollector:11174, autoSyncNow:11196, _autoSyncCore:11202, _autoSyncSteps:11207, _deltaSyncSteps:11246, _afterSyncSteps:11277, probeConnectivity:11293, _markOnlineFromRequest:11302, setOnlineState:11306, _startProbeInterval:11315, attachOnlineListeners:11327, logAudit:11350, auditRowHtml:11366, renderAuditList:11372, refreshAuditScreen:11377, exportAuditLog:11400, simIgnoreSet:11420, simPairKey:11421, findSimilarGroups:11422, mergePersons:11472, openRapidEntry:11498, rapidChain:11503, rapidFinalName:11508, rapidRenderFamily:11515, rapidSetRel:11523, rapidSetGender:11532, rapidPreview:11536, rapidSuggest:11542, rapidCheckNid:11567, rapidSave:11591, rapidRenderLog:11647, rapidUndo:11651, rapidRefreshDv:11701, qbTest:11737, qbRun:11761, qbValueControl:11780, qbRender:11786, qbSavedList:11811, qbRenderPresets:11812, qbSave:11823, qbExport:11830, setMinVersionToCurrent:11858, checkMinVersion:11863, secureLocalUsers:11877, getFirestoreRules:11890, copyTextSafe:11891, securityStatus:11900, runSecurityUpgrade:11917, openSecurityPanel:11975, showForceUpdate:12026, haptic:12045, vtMorph:12047, enableSheetDrag:12064, moveNavIndicator:12097, navShared:12284, cpRecent:12301, cpRemember:12302, cpHl:12308, gsActions:12314, openGlobalSearch:12337, refreshAdminDashboard:12457, renderCampQualityRanking:12539, openCategoryList:12552, categoryCardHtml:12557, attachCategoryCardEvents:12571, downloadCategoryExcel:12614, exportAllCampsExcel:12662, refreshCampsAdminScreen:12701, deleteCampEntirely:12743, refreshAccountsScreen:12821, openDelegateActivityLog:12870, clearCampBeneficiaries:12911, openEditUserModal:12952, deleteUserAccount:13018, accountTypePickerHtml:13050, bindAccountTypePicker:13055, campOptionsHtml:13062, openNewUserModal:13065, createNewUser:13115, resetUserPassword:13172, toggleUserActive:13193, themeSegHtml:13212, bindThemeSeg:13216, openVulnWeightsEditor:13224, sendPushNotification:13280, isPwaEnv:13293, urlBase64ToUint8Array:13296, subscribeWebPush:13302, openSendNotificationComposer:13324, refreshSettingsScreen:13357, cleanOrphanedNationalIndex:13520, openChangePasswordModal:13540, exportFullBackup:13559, silentBackup:13571, restoreFromBackup:13579, gzipBytes:13602, gunzipText:13607, restorePayload:13670, openTrashModal:13714, openBackupsModal:13816, openBackupRestore:13847, currentBeneficiaryPerson:13925, setSwitch:13941, getSwitch:13942, bindSwitchReveal:13943, benActiveTab:13948, switchBenTab:13949, benPersistPerson:13954, refreshBeneficiaryScreen:13962, benFamilyGroupsHtml:14074, renderBenCompleteness:14086, focusField:14126, renderBenPushState:14133, openBenMemberModal:14151, benLogoutConfirm:14317, afterLoginHook:14325, isNewerVersion:14333, showUpdateBanner:14341, openUpdateDetailsModal:14358, maybeCheckForUpdate:14381, checkForUpdate:14387, checkJustUpdated:14402, setSplashStatus:14413, showSplashFatalError:14418, performInitialPull:14437, campMetrics:14514, openFamilyTransfer:14646, openTransferLog:14724, importPreflight:14746, boot:15049, isAppInstalled:15093, isIosSafari:15096, renderPwaInstallBanner:15101, triggerPwaInstall:15121, tryAutoSubscribeWebPush:15149, authed:15204, tok:15205, isAdmin:15206, isStaff:15207, isBen:15208, inCamp:15209, staffOf:15210, isSelf:15211, selfSafeUpdate:15215, pulseMapOk:15308

# Role gating lines

2143: const admin = State.user.role === 'admin';
2202: const admin = u.role === 'admin', own = u.campId;
3185: const u = State.user, admin = u && u.role === 'admin';
3234: if(u.role === 'admin'){ await pullCampsFromServer(); await Promise.all(before.rows.filter(r=>r.pending || r.dirty || r.tombs).map(r=>pullCampPersonsBl
3241: if(!State.user || State.user.role === 'beneficiary') return;
3788: function guardRole(allowed){
3818: if(State.user.role === 'beneficiary'){ bar.hidden = true; document.getElementById('fab-wrap').hidden = true; return; }
3832: if(target === 'screen-dash' && State.user.role === 'admin'){ State.adminViewingCampId = null; }
3843: const isAdminInCamp = State.user?.role === 'admin' && !!State.adminViewingCampId;
3982: return users.find(u=>u.role === 'beneficiary' && (u.personId === p.id || (nid && normUsername(u.username) === nid))) || null;
4043: if(State.user && State.user.role === 'beneficiary' && State.screen === 'screen-beneficiary' && needsForcedPwChange(State.user)) openForcedPasswordModa
4047: const home = State.user.role === 'beneficiary' ? 'screen-beneficiary' : 'screen-dash';
4057: } else if(State.online && State.user.role === 'beneficiary' && State.user.campId){
4066: if(State.user.role === 'beneficiary' && needsForcedPwChange(State.user)) openForcedPasswordModal();
4099: function currentCampId(){ return State.user.role === 'admin' ? State.adminViewingCampId : State.user.campId; }
4105: const ck = esc((State.user?.role==='admin' && !State.adminViewingCampId ? 'g:' : 'c:') + (key||label));
4332: if(!p || !p.isFamilyHead || State.user?.role === 'beneficiary') return '';
4446: if(State.user.role === 'admin' && State.adminViewingCampId !== p.campId) State.adminViewingCampId = p.campId;
4477: if(!guardRole(['admin','delegate','dataentry'])) return;
4478: const campId = currentCampId(), isAdmin = State.user.role === 'admin';
4720: if(!guardRole(['admin','delegate','dataentry'])) return;
4787: function aidDetailHtml(p, canEdit){
4790: return `<div class="section-title"><h2>المساعدات</h2>${canEdit ? '<button class="btn btn-outline btn-sm" id="pd-aid-add"><i class="fa-solid fa-plus"><
4827: if(!State.user || State.user.role === 'beneficiary') return;
4963: if(State.user?.role === 'admin' && !State.adminViewingCampId){ openCategoryList(key); return; }
4973: if(!guardRole(['admin','delegate'])) return;
4976: if(State.user.role === 'admin' && State.online) await pullCampPersonsBlob(campId);
4979: document.getElementById('dash-user-role').textContent = roleHeaderText(State.user) + (State.user.role==='admin' ? ' — عرض مخيم' : '');
4980: document.getElementById('dash-back-btn').hidden = !(State.user.role==='admin' && State.adminViewingCampId);
4998: if(State.user.role === 'admin' && campId){
5150: const isGlobalAdminHome = State.user?.role === 'admin' && !State.adminViewingCampId;
5161: if(State.user.role==='admin' && !State.adminViewingCampId){ await refreshAdminDashboard(); return; }
5162: if(State.user.role==='dataentry'){ await refreshDataEntryDashboard(); return; }
5175: const show = State.user?.role === 'admin' && !!State.adminViewingCampId;
5180: if(importBack) importBack.hidden = !(State.user?.role === 'admin' && (State.adminViewingCampId || State.reconcileMode));
5396: if(!guardRole(['admin','delegate','dataentry'])) return;
5493: const canEdit = ['admin','delegate','dataentry'].includes(State.user.role);
5494: const canDelete = ['admin','delegate'].includes(State.user.role);
5533: ${staffView ? aidDetailHtml(p, canEdit) : ''}
5539: ${canEdit?'<button class="btn btn-outline" id="pd-edit"><i class="fa-solid fa-pen"></i> تعديل</button>':''}
5540: ${canEdit?'<button class="btn btn-outline" id="pd-tags"><i class="fa-solid fa-tags"></i> التصنيفات</button>':''}
5541: ${State.user.role === 'admin' ? '<button class="btn btn-outline" id="pd-move"><i class="fa-solid fa-right-left"></i> نقل الأسرة</button>' : ''}
5542: ${canDelete?'<button class="btn btn-danger" id="pd-delete"><i class="fa-solid fa-trash"></i> حذف</button>':''}
5597: const linkedT = p.isFamilyHead ? (await dbGetAll('users')).filter(u=>u.role==='beneficiary' && (u.personId===p.id || (nidT && u.username===nidT))) : [
5611: const linked = (await dbGetAll('users')).filter(u=>u.role==='beneficiary' && (u.personId===p.id || (nidN && u.username===nidN)));
5785: if(!guardRole(['admin','delegate','dataentry'])) return;
5889: nav(State.user.role==='dataentry' ? 'screen-dash' : 'screen-persons');
6039: nav(State.user.role==='dataentry' ? 'screen-dash' : 'screen-persons');
6046: document.getElementById('fab-import').addEventListener('click', ()=>{ if(guardRole(['admin','delegate','dataentry'])) nav('screen-import'); });
6505: const r = await this.run(role === 'admin' ? null : [State.user.campId]);
6657: const u = users.find(x=>x.role==='beneficiary' && (x.personId===person.id || (oldNid && x.username===oldNid) || (newNid && x.username===newNid)));
6713: if(out.role === 'beneficiary' && out.mustChangePassword === undefined && out.passwordPlain === DEFAULT_BEN_PW) out.mustChangePassword = true;
7014: const u = (users||[]).find(x=>x.role==='beneficiary' && x.personId===head.id);
7057: const u = users.find(x=>x.role==='beneficiary' && x.personId===h.id);
7083: const r = await this.run(role === 'admin' ? null : [State.user.campId]);
8007: if(role === 'skip') return;
8010: if(role === 'extra'){ R.extra[i] = c; return; }
8011: if(role === 'sNid' || role === 'gNid') R[role] = normNid(c);
8012: else if(role === 'dob' || role === 'due' || role === 'pregStart') R[role] = _parseDateAny(raw);
8013: else if(role === 'phone' || role === 'phone2') R[role] = toLatinDigits(c).replace(/\D/g,'').replace(/^(5\d{8})$/, '0$1');
8014: else if(role === 'gender') R.gender = raw;
8299: function catScopeCampId(){ return State.user.role === 'admin' ? (State.adminViewingCampId || null) : State.user.campId; }
8301: if(!guardRole(['admin','delegate','dataentry'])) return;
8485: if(!guardRole(['admin','delegate','dataentry'])) return;
8533: if(!guardRole(['admin','delegate','dataentry'])) return;
8828: if(!guardRole(['admin'])) return;
8834: if(!guardRole(['admin','delegate','dataentry'])) return;
9796: if(!guardRole(['admin','delegate'])) return;
9981: if(!guardRole(['admin','delegate'])) return;
9983: if(State.user.role === 'delegate'){
10036: if(!guardRole(['admin','delegate','beneficiary'])) return;
10039: State.chatReturnScreen = returnScreen || (State.user.role==='beneficiary' ? 'screen-beneficiary' : 'screen-messages-list');
10042: benBtn.hidden = isBenThread(threadId) || State.user.role==='beneficiary' || threadId==='global';
10080: campId: isBenThread(tid) ? State.chatCampId : (State.user.role==='admin' ? tid : State.user.campId),
10083: if(isBenThread(tid)){ msg.beneficiaryId = tid.slice(4); msg.beneficiaryName = State.user.role==='beneficiary' ? State.user.fullName : msg.beneficiaryN
10124: if(State.user?.role === 'beneficiary'){
10144: if(!guardRole(['admin','delegate'])) return;
10150: allUsers.filter(u=>u.role==='beneficiary' && u.campId===campId).forEach(u=>{ benUsersMap[u.id] = u; });
10181: const bens = allUsers.filter(u=>u.role==='beneficiary' && u.campId===campId);
10233: const bens = allUsers.filter(u=>u.role==='beneficiary' && u.campId);
10290: if(State.user.role === 'admin'){
10304: } else if(State.user.role === 'delegate'){
10629: if(State.user.role === 'beneficiary'){
10674: if(role === 'beneficiary'){ const allow = new Set(benFamIds()); ids = ids.filter(x=>allow.has(x)); }
10913: if(u.role === 'admin') return {list: await fsListDocuments('users', 300), scope: ()=>true};
10936: if(opts && opts.campIds && State.user.role === 'admin'){
10940: } else if(State.user.role === 'admin' && !force && startedAt - _lastFullUsersSync < 10*60*1000){
10946: if(State.user.role === 'admin') _lastFullUsersSync = startedAt;
11059: if(State.user.role === 'beneficiary') return await pullBenMessagesForCamp(campId);
11147: if(State.user.role === 'admin'){
11191: if(State.user && State.user.role === 'admin' && State.online) Trash.list().catch(()=>{});
11218: if(State.user.role === 'admin'){
11225: } else if(State.user.role === 'delegate' && State.user.campId){
11230: } else if(State.user.role === 'beneficiary' && State.user.campId){
11247: const d = Pulse.diff(pr), u = State.user, admin = u.role === 'admin';
11281: if(State.user.role === 'admin') Snapshots.maybeTake((await dbGetAll('camps')).filter(c=>c.id!=='HQ').map(c=>c.id)).catch(e=>console.error(e));
11354: campId: user?.role==='admin' ? (State.adminViewingCampId || targetId || 'HQ') : (user?.campId||''),
11378: if(!guardRole(['admin','delegate'])) return;
11379: let entries = State.user.role==='admin' ? await dbGetAll('changelog') : await dbIndexAll('changelog','campId', State.user.campId);
11387: const searchBox = State.user.role==='admin'
11391: if(State.user.role==='admin'){
11437: if(!guardRole(['admin','delegate','dataentry'])) return;
11499: if(!guardRole(['admin','delegate','dataentry'])) return;
11668: if(!guardRole(['admin','delegate','dataentry'])) return;
11683: nav(State.user.role === 'dataentry' ? 'screen-dash' : 'screen-persons');
11848: if(!guardRole(['admin'])) return;
11880: const inScope = u => me.role === 'admin' || u.id === me.id || (me.role !== 'beneficiary' && !!me.campId && u.campId === me.campId);
11908: st.token = Auth.valid() && Auth.claims?.role === 'admin';
11935: if(u.role === 'beneficiary'){
11976: if(!guardRole(['admin'])) return;
12150: if(State.user.role === 'admin') localStorage.setItem('grcs_last_admin_pull', '0');
12182: if(role === 'admin') a.push({k:'move', ic:'fa-right-left', l:'نقل'});
12338: if(!State.user || State.user.role === 'beneficiary' || document.getElementById('gsearch')) return;
12339: const role = State.user.role, admin = role === 'admin';
12469: if(!guardRole(['admin'])) return;
12582: if(!guardRole(['admin'])) return;
12702: if(!guardRole(['admin'])) return;
12812: if(!guardRole(['admin'])) return;
12822: if(!guardRole(['admin'])) return;
12859: ${u.role==='delegate' && u.campId ? `<button class="btn btn-outline btn-sm w-full mt-8" data-action="clearben" data-campid="${esc(u.campId)}" data-cam
12921: const bens = (await dbGetAll('users')).filter(u=>u.role==='beneficiary' && u.campId===campId);
12962: <div class="field" id="eu-camp-wrap" ${u.role==='admin'?'hidden':''}><label id="eu-camp-label">${u.role==='dataentry'?'المخيم الذي يُدخل بياناته':'الم
12970: m.querySelector('#eu-camp-wrap').hidden = role === 'admin';
12971: m.querySelector('#eu-camp-label').textContent = role === 'dataentry' ? 'المخيم الذي يُدخل بياناته' : 'المخيم';
12994: if(u.role === 'beneficiary'){
13021: const isStaff = (u.role==='delegate' || u.role==='dataentry') && u.campId;
13025: bens = users.filter(x=>x.role==='beneficiary' && x.campId===u.campId);
13068: const withDelegate = new Set(users.filter(u=>u.role==='delegate').map(u=>u.campId));
13103: m.querySelector('#nu-save').disabled = role === 'dataentry' && !camps.length;
13105: (m.querySelector(role==='delegate' ? '#nu-newCampName' : '#nu-fullName'))?.focus({preventScroll:true});
13125: if(role === 'delegate'){
13138: } else if(role === 'dataentry'){
13153: if(role === 'dataentry' && campId){
13156: } else if(role === 'delegate' && !newCamp){
13164: State.accTab = role === 'admin' ? 'admin' : 'staff';
13184: if(u.role === 'beneficiary') u.mustChangePassword = true; // كلمة يعرفها الطاقم = أولية — يُجبَر المستفيد على تغييرها
13206: if(State.online && State.user?.role === 'admin' && Date.now() - _lastFullUsersSync > 2*60*1000){
13225: if(!guardRole(['admin'])) return;
13358: if(!guardRole(['admin','delegate','dataentry'])) return;
13391: if(State.user.role === 'delegate' || (State.user.role === 'admin' && State.adminViewingCampId)){
13397: if(State.user.role === 'delegate' && State.user.campId){
13404: if(State.user.role === 'admin'){
13438: <p class="caption mb-0">لا يمكن للتطبيق التحقق من حالة تسجيل هذا الجهاز من الداخل. إذا لم تصلك الإشعارات: تأكد من تفعيل الإشعارات لهذا التطبيق من إعدا
13448: ${State.user.role === 'admin' ? '<button class="btn btn-outline mt-8" id="st-tflog"><i class="fa-solid fa-right-left"></i> سجل نقل الأسر بين المخيمات<
13455: if(State.user.role === 'admin'){
13639: const rows = u.role === 'admin'
13735: <div class="caption">${esc(k.l)}${counts?' · '+esc(counts):''}${State.user.role==='admin' && it.campId!=='HQ' ? ' · '+esc(campNames[it.campId]||it.cam
13769: const u = State.user, admin = u.role === 'admin';
13854: const camps = (snap.camps||[]).filter(c=>State.user.role === 'admin' || c.id === State.user.campId);
13887: if(!guardRole(['admin','delegate'])) return;
13963: if(!guardRole(['beneficiary'])) return;
14157: const canDelete = !isNew && !isWife && !m0.isFamilyHead;
14179: ${canDelete ? `<button class="btn btn-danger" id="bm-del"><i class="fa-solid fa-trash"></i> حذف</button>` : ''}
14362: const canNotifyAll = State.user && State.user.role === 'admin' && PUSH_CONFIG.workerUrl;
14446: if(role === 'admin'){
14466: } else if(role === 'beneficiary'){
14557: out.users = (await dbGetAll('users')).filter(u=>u.role === 'beneficiary' && (out.ids.has(u.personId) || (nidH && normUsername(u.username) === nidH && 
14958: ok('NatRef: أدرج رب الأسرة والزوجة', ix.size === 2 && ix.get('400000009')?.role === 'wife' && fam.get('123456782')?.length === 2, ix.size);
15206: function isAdmin() { return authed() && tok().role == 'admin'; }
15208: function isBen() { return authed() && tok().role == 'beneficiary'; }
15220: allow get: if isAdmin() || isSelf(uid)
15222: allow list: if isAdmin() || (isStaff() && resource.data.campId == tok().camp);
15223: allow create: if isAdmin()
15224: || (staffOf(request.resource.data.campId) && request.resource.data.role == 'beneficiary');
15225: allow update: if isAdmin()
15226: || (staffOf(resource.data.campId) && resource.data.role == 'beneficiary'
15227: && request.resource.data.role == 'beneficiary' && request.resource.data.campId == resource.data.campId)
15229: allow delete: if isAdmin() || (staffOf(resource.data.campId) && resource.data.role == 'beneficiary');
15234: allow get: if isAdmin() || isSelf(uid)
15235: || (staffOf(resource.data.campId) && resource.data.role == 'beneficiary');
15236: allow list: if isAdmin();
15237: allow create: if isAdmin() || (isSelf(uid) && request.resource.data.uid == uid)
15238: || (staffOf(request.resource.data.campId) && request.resource.data.role == 'beneficiary');
15239: allow update: if isAdmin() || (isSelf(uid) && request.resource.data.uid == uid)
15240: || (staffOf(resource.data.campId) && resource.data.role == 'beneficiary'
15241: && request.resource.data.role == 'beneficiary' && request.resource.data.campId == resource.data.campId);
15242: allow delete: if isAdmin() || (staffOf(resource.data.campId) && resource.data.role == 'beneficiary');
15246: allow get: if isAdmin() || inCamp(campId);
15247: allow list: if isAdmin();
15248: allow create, delete: if isAdmin();
15249: allow update: if isAdmin() || staffOf(campId);
15253: allow get: if isAdmin() || staffOf(campId)
15254: || (isBen() && inCamp(campId) && (!('blob' in resource.data) || resource.data.blob == ''));
15255: allow list: if isAdmin() || staffOf(campId);
15256: allow write: if isAdmin() || staffOf(campId);
15260: allow read, write: if isAdmin() || staffOf(campId)
15261: || (isBen() && inCamp(campId) && famId in tok().fams);
15264: allow read, write: if isAdmin() || staffOf(campId);
15267: allow read: if isAdmin() || staffOf(campId)
15268: || (isBen() && inCamp(campId) && resource.data.threadId == 'ben_' + request.auth.uid);
15269: allow create: if isAdmin() || staffOf(campId)
15270: || (isBen() && inCamp(campId) && request.resource.data.threadId == 'ben_' + request.auth.uid);
15271: allow update, delete: if isAdmin() || staffOf(campId);
15274: allow create: if isAdmin() || inCamp(campId);
15275: allow read, update, delete: if isAdmin() || staffOf(campId);
15278: allow read, write: if isAdmin() || staffOf(campId);
15281: allow read, write: if isAdmin();
15286: allow read, write: if isAdmin() || isStaff();
15291: allow get: if isAdmin() || isStaff();
15292: allow list: if isAdmin();
15293: allow create: if isAdmin() || staffOf(request.resource.data.campId);
15294: allow update: if isAdmin() || (staffOf(resource.data.campId) && staffOf(request.resource.data.campId));
15295: allow delete: if isAdmin() || inCamp(resource.data.campId);
15300: allow write: if isAdmin();
15303: allow read, write: if isAdmin() || isStaff();
15315: allow write: if isAdmin() || (doc == 'pulse' && authed() && tok().camp is string && tok().camp != ''
15323: allow get, delete: if isAdmin() || staffOf(resource.data.campId);
15324: allow list: if isAdmin() || (isStaff() && resource.data.campId == tok().camp);
15325: allow create: if isAdmin() || staffOf(request.resource.data.campId);