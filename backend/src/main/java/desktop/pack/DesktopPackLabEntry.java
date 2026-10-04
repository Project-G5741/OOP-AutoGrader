package com.eiu.capstone.backend.desktop.pack;

import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;

public record DesktopPackLabEntry(DesktopPackLabMeta lab, LabRubricSnapshot rubric) {}
