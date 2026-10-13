public class TextBlockSpace {
  private static final String FILTER = """
                    (l.deadline_date IS NULL
                     OR s.submitted_at <= ((CAST(l.deadline_date AS timestamp) + TIME '23:59:59') AT TIME ZONE 'Asia/Ho_Chi_Minh'))
                    """;
  private static final String EXISTS = """
            EXISTS (
                SELECT 1
                FROM submission_challenge_result scr_x
                WHERE scr_x.submission_id = s.id AND scr_x.challenge_id = :challengeId
            )
            """;

  public static void main(String[] args) {
    String sql = """
                WHERE s.user_id = u.id AND s.lab_id = l.id
                  AND\s""" + FILTER + """
                  AND\s""" + EXISTS + """
                )
                """;
    System.out.println(sql.replace(" ", "·"));
    System.out.println(sql.contains("ANDEXISTS") ? "STILL_GLUED" : "NOT_GLUED");
    System.out.println(sql.contains("AND EXISTS") ? "HAS_AND_EXISTS" : "MISSING_AND_EXISTS");
  }
}
