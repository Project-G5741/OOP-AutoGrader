package com.eiu.capstone.backend.grading.mmd.ast;

public sealed interface MmdTopLevelNode permits
        MmdClassNode,
        MmdRelationNode,
        MmdColonMemberNode,
        MmdStandaloneStereotypeNode,
        MmdNamespaceNode,
        MmdIgnoredDirectiveNode {

    int line();
}
