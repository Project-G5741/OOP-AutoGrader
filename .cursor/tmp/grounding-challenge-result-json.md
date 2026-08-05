# Grounding Dossier: challenge_result JSON via submission APIs
# Extraction only — verbatim quotes with file:line pointers
# Generated for requirements brainstorm

---

## 1. Submission upload API endpoint and response DTOs

### SubmissionController.upload
`backend/src/main/java/com/eiu/capstone/backend/controller/SubmissionController.java`

```80:81:backend/src/main/java/com/eiu/capstone/backend/controller/SubmissionController.java
    @PostMapping("/{labId}/{attemptNumber}/upload")
    public ResponseEntity<SubmissionUploadResponse> upload(
```

```126:156:backend/src/main/java/com/eiu/capstone/backend/controller/SubmissionController.java
            long gradeStart = System.currentTimeMillis();
            BigDecimal score = gradingService.gradeSubmission(submission, rubric, uploadResult.challenges);
            long gradeMs = System.currentTimeMillis() - gradeStart;

            submission.setScore(score);
            submission = labSubmissionRepository.save(submission);

            updateStudentProgress(userAccount, lab, submission, score);

            mmdPersistenceHook.onUploadComplete(irn, requestId, uploadResult.mmdByChallenge);

            List<ChallengeUploadResult> challengeResults = uploadResult.challenges.stream()
                    .map(c -> new ChallengeUploadResult(
                            c.challengeName,
                            uploadResult.mmdByChallenge.getOrDefault(c.challengeName, List.of()).size(),
                            c.classFileCount))
                    .collect(Collectors.toList());

            if (timingLog) {
                long totalMs = System.currentTimeMillis() - totalStart;
                System.out.printf("grading_timing rubric_ms=%d process_ms=%d grade_ms=%d total_ms=%d%n",
                        rubricMs, processMs, gradeMs, totalMs);
            }

            return ResponseEntity.ok(new SubmissionUploadResponse(
                    submission.getId(),
                    irn,
                    requestId,
                    challengeResults,
                    submission.getScore()
            ));
```

### SubmissionUploadResponse
`backend/src/main/java/com/eiu/capstone/backend/DTO/SubmissionUploadResponse.java`

```7:28:backend/src/main/java/com/eiu/capstone/backend/DTO/SubmissionUploadResponse.java
public class SubmissionUploadResponse {

    private final UUID submissionId;
    private final String irn;
    private final String requestId;
    private final List<ChallengeUploadResult> challenges;
    private final BigDecimal score;

    public SubmissionUploadResponse(UUID submissionId, String irn, String requestId,
                                     List<ChallengeUploadResult> challenges, BigDecimal score) {
        this.submissionId = submissionId;
        this.irn = irn;
        this.requestId = requestId;
        this.challenges = challenges;
        this.score = score;
    }

    public UUID getSubmissionId() { return submissionId; }
    public String getIrn() { return irn; }
    public String getRequestId() { return requestId; }
    public List<ChallengeUploadResult> getChallenges() { return challenges; }
    public BigDecimal getScore() { return score; }
}
```

### ChallengeUploadResult
`backend/src/main/java/com/eiu/capstone/backend/DTO/ChallengeUploadResult.java`

```3:17:backend/src/main/java/com/eiu/capstone/backend/DTO/ChallengeUploadResult.java
public class ChallengeUploadResult {

    private final String challengeName;
    private final int mmdFileCount;
    private final int classFileCount;

    public ChallengeUploadResult(String challengeName, int mmdFileCount, int classFileCount) {
        this.challengeName = challengeName;
        this.mmdFileCount = mmdFileCount;
        this.classFileCount = classFileCount;
    }

    public String getChallengeName() { return challengeName; }
    public int getMmdFileCount() { return mmdFileCount; }
    public int getClassFileCount() { return classFileCount; }
}
```

### DropZone frontend upload call
`frontend/src/components/ui/DropZone.jsx`

```133:149:frontend/src/components/ui/DropZone.jsx
      // Backend upserts on (user, lab, attemptNumber) - this must be the
      // next unused attempt number for this student+lab.
      const res = await fetch(`${API_BASE}/api/submissions/${labId}/${attemptNumber}/upload`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
        body: formData,
      });

      if (!res.ok) {
        const message = await readUploadErrorMessage(res);
        throw new Error(message);
      }

      const data = await res.json();
      console.log('Upload successful:', data);
```

---

## 2. Challenge read APIs (post-upload grade display)

### ChallengeController
`backend/src/main/java/com/eiu/capstone/backend/controller/ChallengeController.java`

```15:17:backend/src/main/java/com/eiu/capstone/backend/controller/ChallengeController.java
@RestController
@RequestMapping("/api/labs/{labId}/challenges")
@CrossOrigin // adjust/remove to match your existing CORS config
```

```32:37:backend/src/main/java/com/eiu/capstone/backend/controller/ChallengeController.java
    /** Powers the Challenges sidebar. studentId is optional so the list still loads (with score=null) if omitted. */
    @GetMapping
    public List<ChallengeDTO> getChallenges(@PathVariable UUID labId,
                                             @RequestParam(required = false) UUID studentId) {
        return challengeService.getChallengesForLab(labId, studentId);
```

```39:53:backend/src/main/java/com/eiu/capstone/backend/controller/ChallengeController.java
    /** Powers the "MMD" tab. Returns [] when the student has no reference submission yet. */
    @GetMapping("/{challengeId}/mmd")
    public List<MmdClassDTO> getMmd(@PathVariable UUID labId,
                                     @PathVariable UUID challengeId,
                                     @RequestParam(required = false) UUID studentId) {
        return classStructureService.getMmdData(labId, challengeId, studentId);
    }

    /** Powers the "Class" tab. Returns [] when the student has no reference submission yet. */
    @GetMapping("/{challengeId}/class")
    public List<ClassDetailDTO> getClassData(@PathVariable UUID labId,
                                              @PathVariable UUID challengeId,
                                              @RequestParam(required = false) UUID studentId) {
        return classStructureService.getClassData(labId, challengeId, studentId);
```

```55:66:backend/src/main/java/com/eiu/capstone/backend/controller/ChallengeController.java
    /**
     * Powers the 3 stat cards (Current Grade / Total Submissions / Latest
     * Submission). challengeId is accepted for route symmetry with the
     * frontend's existing fetch call but isn't used — stats are tracked per
     * (student, lab) via student_lab_progress, not per challenge.
     */
    @GetMapping("/{challengeId}/stats")
    public StatsDTO getStats(@PathVariable UUID labId,
                              @PathVariable UUID challengeId,
                              @RequestParam(required = false) UUID studentId) {
        return statsService.getStats(labId, studentId);
```

```68:70:backend/src/main/java/com/eiu/capstone/backend/controller/ChallengeController.java
    // NOTE: no /{challengeId}/testcases endpoint yet — there's no table in
    // DbContext.docx backing test cases (input/expectedOutput/hidden flag),
    // so it's skipped per your instruction until that model exists.
```

### ChallengeDTO
`backend/src/main/java/com/eiu/capstone/backend/DTO/ChallengeDTO.java`

```5:11:backend/src/main/java/com/eiu/capstone/backend/DTO/ChallengeDTO.java
/**
 * Matches the frontend's `{ id, name, score }` shape used in the challenges
 * sidebar. `score` is null when the student hasn't submitted anything for
 * this challenge yet — the frontend renders "Not submitted" in that case.
 */
public record ChallengeDTO(UUID id, String name, Integer score) {
}
```

### StatsDTO
`backend/src/main/java/com/eiu/capstone/backend/DTO/StatsDTO.java`

```3:8:backend/src/main/java/com/eiu/capstone/backend/DTO/StatsDTO.java
/**
 * All three fields are null when the student has no student_lab_progress row
 * for the lab yet — the frontend renders "--/--" for each in that case.
 */
public record StatsDTO(Integer currentGrade, Integer totalSubmissions, String latestSubmission) {
}
```

### MmdClassDTO / MmdAttributeDTO
`backend/src/main/java/com/eiu/capstone/backend/DTO/MmdClassDTO.java`

```5:7:backend/src/main/java/com/eiu/capstone/backend/DTO/MmdClassDTO.java
/** One class box in the MMD tab. */
public record MmdClassDTO(String name, List<MmdAttributeDTO> attributes) {
}
```

`backend/src/main/java/com/eiu/capstone/backend/DTO/MmdAttributeDTO.java`

```3:9:backend/src/main/java/com/eiu/capstone/backend/DTO/MmdAttributeDTO.java
/**
 * One row in an MMD class box, e.g. "speed: double" (type="field"),
 * "Vehicle(brand)" (type="constructor"), "move(): void" (type="method").
 * `type` drives the color coding on the frontend (Tick + text color).
 */
public record MmdAttributeDTO(String name, String type, boolean ok) {
}
```

### ClassDetailDTO / ClassFieldDetailDTO
`backend/src/main/java/com/eiu/capstone/backend/DTO/ClassDetailDTO.java`

```5:18:backend/src/main/java/com/eiu/capstone/backend/DTO/ClassDetailDTO.java
/**
 * `type` is e.g. "ABSTRACT CLASS", "CLASS", "INTERFACE" — derived from
 * class_entity.is_abstract + class_entity.declaring_type (resolved via master_data).
 * `status` is one of "success" | "warning" | "error" | "info", computed from
 * how many of this class's fields/constructors/methods were graded correct.
 */
public record ClassDetailDTO(
        String name,
        String type,
        String status,
        List<ClassFieldDetailDTO> fields,
        List<ClassConstructorDetailDTO> constructors,
        List<ClassMethodDetailDTO> methods
) {
}
```

`backend/src/main/java/com/eiu/capstone/backend/DTO/ClassFieldDetailDTO.java`

```3:4:backend/src/main/java/com/eiu/capstone/backend/DTO/ClassFieldDetailDTO.java
public record ClassFieldDetailDTO(String name, String scope, String dataType, boolean ok) {
}
```

---

## 3. What grading produces

### GradingService.gradeSubmission — console output
`backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java`

```66:84:backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java
    public BigDecimal gradeSubmission(LabSubmission submission,
                                      LabRubricSnapshot rubric,
                                      List<SubmissionStorageService.ChallengeResult> challengeFolderResults) {

        String irn = submission.getUser() != null ? submission.getUser().getIrn() : "unknown";
        System.out.println("=========================================");
        System.out.println("Grading submission " + submission.getId() + " (IRN: " + irn + ")");

        GradingService.ExistingResults existing = gradingResultStore.loadExisting(submission);
        GradingComputationResult computed = computeAgainstSnapshot(
                rubric, challengeFolderResults, submission, existing);
        gradingResultStore.save(computed);

        System.out.println("Overall score (simple average across " + computed.challengePercentages.size()
                + " challenge(s)): " + computed.overallScore + " / 100");
        System.out.println("=========================================");

        return computed.overallScore;
    }
```

### GradingService.printChallengeReport — console output
`backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java`

```381:394:backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java
    private void printChallengeReport(ChallengeComputation computation) {
        System.out.println("--- Challenge #" + computation.challengeNumber + ": " + computation.challengeName + " ---");
        System.out.println("  Score: " + computation.percentage + "% | Fully correct: " + computation.fullyCorrect);
        for (ClassGradeReport cr : computation.classReports) {
            String status = !cr.matched ? "MISSING" : (cr.classAttributesCorrect ? "OK" : "class declaration incorrect");
            System.out.println("  Class " + cr.className + ": " + status);
            printIfNotEmpty("    Missing fields", cr.missingFields);
            printIfNotEmpty("    Incorrect fields", cr.incorrectFields);
            printIfNotEmpty("    Missing methods", cr.missingMethods);
            printIfNotEmpty("    Incorrect methods", cr.incorrectMethods);
            printIfNotEmpty("    Missing constructors", cr.missingConstructors);
            printIfNotEmpty("    Incorrect constructors", cr.incorrectConstructors);
        }
    }
```

### GradingService.GradingComputationResult — in-memory result shape
`backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java`

```409:416:backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java
    static class GradingComputationResult {
        List<SubmissionFieldResult> fieldResults;
        List<SubmissionMethodResult> methodResults;
        List<SubmissionConstructorResult> constructorResults;
        List<SubmissionChallengeResult> challengeResults;
        List<BigDecimal> challengePercentages;
        BigDecimal overallScore;
    }
```

### GradingService.ClassGradeReport — in-memory per-class report (not persisted)
`backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java`

```430:440:backend/src/main/java/com/eiu/capstone/backend/grading/GradingService.java
    private static class ClassGradeReport {
        public String className;
        public boolean matched;
        public boolean classAttributesCorrect;
        public List<String> missingFields = new ArrayList<>();
        public List<String> incorrectFields = new ArrayList<>();
        public List<String> missingMethods = new ArrayList<>();
        public List<String> incorrectMethods = new ArrayList<>();
        public List<String> missingConstructors = new ArrayList<>();
        public List<String> incorrectConstructors = new ArrayList<>();
    }
```

### GradingResultStore.save
`backend/src/main/java/com/eiu/capstone/backend/grading/GradingResultStore.java`

```46:52:backend/src/main/java/com/eiu/capstone/backend/grading/GradingResultStore.java
    @Transactional
    void save(GradingService.GradingComputationResult computed) {
        submissionFieldResultRepository.saveAll(computed.fieldResults);
        submissionMethodResultRepository.saveAll(computed.methodResults);
        submissionConstructorResultRepository.saveAll(computed.constructorResults);
        submissionChallengeResultRepository.saveAll(computed.challengeResults);
    }
```

### DB tables — submission_challenge_result
`backend/src/main/java/com/eiu/capstone/backend/model/SubmissionChallengeResult.java`

```17:42:backend/src/main/java/com/eiu/capstone/backend/model/SubmissionChallengeResult.java
@Entity
@Table(
        name = "submission_challenge_result",
        uniqueConstraints = @UniqueConstraint(
                name = "submission_challenge_result_key",
                columnNames = {"submission_id", "challenge_id"}
        )
)
public class SubmissionChallengeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false,
            foreignKey = @ForeignKey(name = "scr_submission_id_fkey"))
    private LabSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false,
            foreignKey = @ForeignKey(name = "scr_challenge_id_fkey"))
    private Challenge challenge;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect = false;
```

### DB tables — submission_field_result
`backend/src/main/java/com/eiu/capstone/backend/model/SubmissionFieldResult.java`

```17:42:backend/src/main/java/com/eiu/capstone/backend/model/SubmissionFieldResult.java
@Entity
@Table(
        name = "submission_field_result",
        uniqueConstraints = @UniqueConstraint(
                name = "submission_field_result_key",
                columnNames = {"submission_id", "field_id"}
        )
)
public class SubmissionFieldResult {
    ...
    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect = false;
```

### lab_submission.score
`backend/src/main/java/com/eiu/capstone/backend/model/LabSubmission.java`

```21:47:backend/src/main/java/com/eiu/capstone/backend/model/LabSubmission.java
@Entity
@Table(
        name = "lab_submission",
        uniqueConstraints = @UniqueConstraint(
                name = "lab_submission_user_lab_attempt_key",
                columnNames = {"user_id", "lab_id", "attempt_number"}
        )
)
public class LabSubmission {
    ...
    @Column(name = "score", nullable = false, precision = 6, scale = 2)
    private BigDecimal score = BigDecimal.ZERO;
```

### grading/AGENTS.md — persistence summary
`backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md`

```66:75:backend/src/main/java/com/eiu/capstone/backend/grading/AGENTS.md
### Result persistence

| Entity | Stores |
|---|---|
| `SubmissionChallengeResult` | Per-challenge score |
| `SubmissionFieldResult` | Field match outcome |
| `SubmissionMethodResult` | Method match outcome |
| `SubmissionConstructorResult` | Constructor match outcome |

Each upload upserts result rows keyed by `(submission_id, element_id)` via `GradingResultStore.loadExisting` + `saveAll`.
```

### SubmissionStorageService.ProcessResult
`backend/src/main/java/com/eiu/capstone/backend/service/SubmissionStorageService.java`

```44:63:backend/src/main/java/com/eiu/capstone/backend/service/SubmissionStorageService.java
    public static class ChallengeResult {
        ...
    }

    public static class ProcessResult {
        public final Path submissionFolder;
        public final List<ChallengeResult> challenges;
        public final Map<String, List<String>> mmdByChallenge;
        ...
    }
```

---

## 4. StudentDashboard.jsx — fetch/display

### Labs fetch
`frontend/src/pages/StudentDashboard.jsx`

```41:61:frontend/src/pages/StudentDashboard.jsx
  // 1. Fetch labs
  useEffect(() => {
    async function fetchLabs() {
      setIsLoadingLabs(true);
      try {
        const res = await fetch(`${API_BASE}/api/labs`);
        ...
        setLabs(data);
```

### Challenges fetch (no studentId param)
`frontend/src/pages/StudentDashboard.jsx`

```63:90:frontend/src/pages/StudentDashboard.jsx
  // 2. Fetch challenges khi lab thay đổi
  useEffect(() => {
    if (!selectedLabId) return;

    async function fetchChallenges() {
      ...
        const res = await fetch(`${API_BASE}/api/labs/${selectedLabId}/challenges`);
        ...
        setChallenges(data);
```

### challenge_result comment + detail fetch gating
`frontend/src/pages/StudentDashboard.jsx`

```92:134:frontend/src/pages/StudentDashboard.jsx
  // 3. Fetch chi tiết (MMD, Class, Testcases, Stats) khi challenge thay đổi
  //
  // Gating rule: the challenges list (fetched above) already tells us, per
  // challenge, whether the student has a submission for it (`ch.score` is
  // null/undefined when there's none — this is the front-end equivalent of
  // the backend's `challenge_result["challenge_N"]` being an empty array).
  // When a challenge has no submission, there is nothing to grade-detail,
  // so we skip the mmd/class/testcase calls entirely instead of hitting the
  // backend for empty results.
  useEffect(() => {
    ...
        const hasSubmissionData =
          currentChallenge?.score !== null && currentChallenge?.score !== undefined;

        if (hasSubmissionData) {
          const mmdRes = await fetch(
            `${API_BASE}/api/labs/${selectedLabId}/challenges/${selectedChallengeId}/mmd`
          );
          ...
          const classRes = await fetch(
            `${API_BASE}/api/labs/${selectedLabId}/challenges/${selectedChallengeId}/class`
          );
          ...
          const testRes = await fetch(
            `${API_BASE}/api/labs/${selectedLabId}/challenges/${selectedChallengeId}/testcases`
          );
        } else {
          setMmdData([]);
          setClassData([]);
          setTestCases([]);
        }

        const statsRes = await fetch(
          `${API_BASE}/api/labs/${selectedLabId}/challenges/${selectedChallengeId}/stats?studentId=${user?.id}`
        );
```

### handleFileUpload TODO
`frontend/src/pages/StudentDashboard.jsx`

```166:169:frontend/src/pages/StudentDashboard.jsx
  const handleFileUpload = async (files, labId, challengeId) => {
    // TODO: Implement file upload to backend
    console.log('Uploading files:', files, 'to lab:', labId, 'challenge:', challengeId);
  };
```

---

## 5. StudentUI.jsx — display

### Props and DropZone wiring
`frontend/src/components/student/StudentUI.jsx`

```51:62:frontend/src/components/student/StudentUI.jsx
  // Dữ liệu challenges/problems
  challenges = [],
  ...
  // Dữ liệu chi tiết cho challenge đã chọn
  mmdData = [],
  classData = [],
  ...
  stats = {
    currentGrade: null,
    totalSubmissions: null,
    latestSubmission: null,
  },
```

```139:148:frontend/src/components/student/StudentUI.jsx
        <div className="mb-4">
          <DropZone
            title="Drop your project files here"
            buttonText="Select Project"
            labId={selectedLabId}
            attemptNumber={(stats.totalSubmissions ?? 0) + 1}
            authToken={user?.accessToken}
            onFilesSelected={(files) => onFileUpload(files, selectedLabId, selectedChallengeId)}
          />
        </div>
```

### Challenge sidebar score display
`frontend/src/components/student/StudentUI.jsx`

```227:234:frontend/src/components/student/StudentUI.jsx
                        {hasValue(ch.score) ? (
                          <p className={`text-xs mt-0.5 font-semibold ${scoreColor(ch.score)}`}>
                            {ch.score} / 100
                          </p>
                        ) : (
                          <p className="text-xs mt-0.5 text-gray-400 dark:text-gray-600">
                            Not submitted
                          </p>
                        )}
```

### Tabs: mmd, class, testcase
`frontend/src/components/student/StudentUI.jsx`

```250:257:frontend/src/components/student/StudentUI.jsx
            <div className="flex border-b border-gray-100 dark:border-gray-700 px-2">
              {['mmd', 'class', 'testcase'].map((t) => (
                ...
                  {t === 'mmd' ? 'MMD' : t === 'class' ? 'Class' : 'Testcase'}
```

---

## 6. challenge_result shape references

### Frontend comment only (no backend JSON key `challenge_result` found in code)
`frontend/src/pages/StudentDashboard.jsx:97` — see section 4 above.

### DB entity name submission_challenge_result (not JSON)
`backend/src/main/java/com/eiu/capstone/backend/model/SubmissionChallengeResult.java:19` — see section 3 above.

### CONCEPTS.md
`CONCEPTS.md`

```11:11:CONCEPTS.md
A persisted per-element grading outcome (field, method, constructor, or challenge) tied to one lab submission. Natural key is submission plus rubric element id; re-grades update the same row.
```

### PendingGradingResults records
`backend/src/main/java/com/eiu/capstone/backend/grading/PendingGradingResults.java`

```5:11:backend/src/main/java/com/eiu/capstone/backend/grading/PendingGradingResults.java
record PendingFieldResult(UUID fieldId, boolean correct) {}

record PendingMethodResult(UUID methodId, boolean correct) {}

record PendingConstructorResult(UUID constructorId, boolean correct) {}

record PendingChallengeResult(UUID challengeId, boolean correct) {}
```

---

## 7. ClassStructureService and result persistence reads

### ClassStructureService — reference submission resolution
`backend/src/main/java/com/eiu/capstone/backend/service/ClassStructureService.java`

```47:50:backend/src/main/java/com/eiu/capstone/backend/service/ClassStructureService.java
    /** Powers the "MMD" tab: one box per class, one line per field/constructor/method. */
    public List<MmdClassDTO> getMmdData(UUID labId, UUID challengeId, UUID studentId) {
        UUID submissionId = resolveReferenceSubmissionId(labId, studentId);
        if (submissionId == null) return List.of();
```

```180:185:backend/src/main/java/com/eiu/capstone/backend/service/ClassStructureService.java
    private UUID resolveReferenceSubmissionId(UUID labId, UUID studentId) {
        if (studentId == null) return null;
        return studentLabProgressRepository.findByUser_IdAndLab_Id(studentId, labId)
                .map(StudentLabProgress::getBestSubmissionId)
                .orElse(null);
    }
```

### ClassStructureService — reads submission_*_result tables
`backend/src/main/java/com/eiu/capstone/backend/service/ClassStructureService.java`

```156:177:backend/src/main/java/com/eiu/capstone/backend/service/ClassStructureService.java
    private Set<UUID> correctFieldIds(UUID submissionId) {
        Set<UUID> ids = new HashSet<>();
        for (SubmissionFieldResult r : submissionFieldResultRepository.findBySubmission_Id(submissionId)) {
            if (r.isCorrect()) ids.add(r.getField().getId());
        }
        return ids;
    }

    private Set<UUID> correctMethodIds(UUID submissionId) {
        ...
        for (SubmissionMethodResult r : submissionMethodResultRepository.findBySubmission_Id(submissionId)) {
            if (r.isCorrect()) ids.add(r.getMethod().getId());
        }
        ...
    }

    private Set<UUID> correctConstructorIds(UUID submissionId) {
        ...
        for (SubmissionConstructorResult r : submissionConstructorResultRepository.findBySubmission_Id(submissionId)) {
            if (r.isCorrect()) ids.add(r.getConstructor().getId());
        }
        ...
    }
```

### ChallengeService — sidebar score from submission_*_result (not submission_challenge_result)
`backend/src/main/java/com/eiu/capstone/backend/service/ChallengeService.java`

```57:62:backend/src/main/java/com/eiu/capstone/backend/service/ChallengeService.java
    /**
     * The submission used to grade the sidebar score (and the MMD/Class
     * tabs, see ClassStructureService) is the student's BEST submission for
     * the lab — student_lab_progress.best_submission_id. Swap this for a
     * "most recent attempt" lookup if you'd rather always show the latest try.
     */
```

```70:75:backend/src/main/java/com/eiu/capstone/backend/service/ChallengeService.java
    /**
     * There's no single numeric "challenge score" column in the schema —
     * only per-submission is_correct flags per field/method/constructor.
     * This computes the percentage of the challenge's expected members that
     * were graded correct in the reference submission, e.g. "92/100".
     */
```

```89:106:backend/src/main/java/com/eiu/capstone/backend/service/ChallengeService.java
        Set<UUID> correctFieldIds = new HashSet<>();
        for (SubmissionFieldResult r : submissionFieldResultRepository.findBySubmission_Id(submissionId)) {
            if (r.isCorrect()) correctFieldIds.add(r.getField().getId());
        }
        ...
        long correct = fields.stream().filter(f -> correctFieldIds.contains(f.getId())).count()
                + methods.stream().filter(m -> correctMethodIds.contains(m.getId())).count()
                + constructors.stream().filter(c -> correctConstructorIds.contains(c.getId())).count();

        return Math.round((float) (correct * 100.0 / total));
```

### StatsService — student_lab_progress
`backend/src/main/java/com/eiu/capstone/backend/service/StatsService.java`

```23:27:backend/src/main/java/com/eiu/capstone/backend/service/StatsService.java
    /**
     * Stats are tracked per (student, lab) via student_lab_progress, not per
     * challenge — there's no challengeId param here even though the
     * frontend's route includes one; the controller just doesn't forward it.
     */
```

```38:52:backend/src/main/java/com/eiu/capstone/backend/service/StatsService.java
    private StatsDTO toDto(StudentLabProgress progress) {
        Integer currentGrade = progress.getHighestScore() == null
                ? null
                : Math.round(progress.getHighestScore().floatValue());

        Integer totalSubmissions = progress.getAttemptsCount();
        ...
        return new StatsDTO(currentGrade, totalSubmissions, latestSubmission);
    }
```
