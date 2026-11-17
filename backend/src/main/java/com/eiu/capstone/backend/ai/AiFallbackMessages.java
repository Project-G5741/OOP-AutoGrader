package com.eiu.capstone.backend.ai;

import java.util.ArrayList;
import java.util.List;

final class AiFallbackMessages {

    private AiFallbackMessages() {}

    static ChallengeAiFeedbackPayload forChallenge(
            String challengeName,
            List<FailingClassInfo> failingClasses) {
        String summary = "Some classes in " + (challengeName != null ? challengeName : "this challenge")
                + " require revisions to satisfy the object-oriented design and requirements.";
        List<ClassGuidanceItem> classItems = new ArrayList<>();
        if (failingClasses != null) {
            for (FailingClassInfo failing : failingClasses) {
                List<GuidanceDetail> details = new ArrayList<>();
                if (failing.compileError() != null && !failing.compileError().isBlank()) {
                    details.add(new GuidanceDetail(
                            "Compilation & Syntax",
                            "This class encountered a compilation error. Check compiler messages, type names, imports, and return types.",
                            "Are all referenced types and identifiers declared and spelled correctly?"));
                }
                if (failing.fieldIssues() != null && !failing.fieldIssues().isEmpty()) {
                    details.add(new GuidanceDetail(
                            "Attribute Encapsulation & Types",
                            "Attributes in this class have missing names, wrong types, or incorrect access modifiers.",
                            "Are class fields properly encapsulated with private visibility and appropriate datatypes?"));
                }
                if (failing.constructorIssues() != null && !failing.constructorIssues().isEmpty()) {
                    details.add(new GuidanceDetail(
                            "Constructors & Object Initialization",
                            "Constructors do not match expected parameter types or count.",
                            "Does your constructor take all required parameters and initialize class state properly?"));
                }
                if (failing.methodIssues() != null && !failing.methodIssues().isEmpty()) {
                    details.add(new GuidanceDetail(
                            "Methods & Class Behaviors",
                            "Methods in this class have incorrect return types, parameters, or names.",
                            "Do your method signatures match the expected behavior and return types?"));
                }
                if (details.isEmpty()) {
                    details.add(new GuidanceDetail(
                            "Class Structure & Design",
                            "Review the declaration and structure of this class to ensure all specifications are met.",
                            "Does this class implement all required OOP principles and responsibilities?"));
                }
                classItems.add(new ClassGuidanceItem(
                        failing.className(),
                        failing.classType() != null ? failing.classType() : "CLASS",
                        failing.status() != null ? failing.status() : "error",
                        failing.compileError(),
                        details));
            }
        }
        return new ChallengeAiFeedbackPayload(summary, classItems, "en");
    }

    static ConceptualHintPayload forArea(AiLearningArea area, String language) {
        boolean vi = language != null && language.toLowerCase().startsWith("vi");
        return switch (area) {
            case FIELD_MODELING -> new ConceptualHintPayload(
                    vi ? "Phản hồi AI tạm thời không khả dụng an toàn." : "AI feedback is temporarily unavailable in a safe form.",
                    area.name(),
                    "Data modelling",
                    vi
                            ? "Lớp của bạn nên lưu những thông tin nào để mô tả đúng đối tượng trong bài?"
                            : "What information should this class store to meaningfully represent its real-world counterpart?",
                    vi
                            ? "Gợi ý mang tính khái niệm — hãy suy nghĩ về trách nhiệm dữ liệu, không sao chép đáp án."
                            : "This is a Conceptual prompt — reason about data responsibilities rather than copying an answer.",
                    language == null ? "en" : language);
            case METHODS -> new ConceptualHintPayload(
                    vi ? "Phản hồi AI tạm thời không khả dụng an toàn." : "AI feedback is temporarily unavailable in a safe form.",
                    area.name(),
                    "Object responsibilities",
                    vi
                            ? "Ngoài việc lưu dữ liệu, đối tượng này nên chịu trách nhiệm những hành vi nào?"
                            : "Beyond storing data, what behaviours should this object be responsible for?",
                    vi
                            ? "Gợi ý mang tính khái niệm về trách nhiệm của đối tượng."
                            : "This Conceptual hint focuses on object responsibilities.",
                    language == null ? "en" : language);
            case CONSTRUCTORS -> new ConceptualHintPayload(
                    vi ? "Phản hồi AI tạm thời không khả dụng an toàn." : "AI feedback is temporarily unavailable in a safe form.",
                    area.name(),
                    "Object initialisation",
                    vi
                            ? "Ngay sau khi tạo đối tượng, trạng thái ban đầu nào cần đúng và đủ?"
                            : "Right after an object is created, what should be true about its initial state?",
                    vi
                            ? "Gợi ý mang tính khái niệm về khởi tạo đối tượng."
                            : "This Conceptual hint focuses on initial object state.",
                    language == null ? "en" : language);
        };
    }
}
