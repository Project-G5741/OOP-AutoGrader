package com.eiu.capstone.backend.ai;

final class AiFallbackMessages {

    private AiFallbackMessages() {}

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
