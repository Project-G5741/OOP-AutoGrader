-- Tag master_data rows for lecturer structure editor combo boxes.
-- Run once against environments where category is still UNSPECIFIED.

UPDATE master_data
SET category = 'SCOPE'
WHERE category = 'UNSPECIFIED'
  AND UPPER(name) IN ('PUBLIC', 'PRIVATE', 'PROTECTED', 'DEFAULT', 'PACKAGE-PRIVATE', 'PACKAGE_PRIVATE');

UPDATE master_data
SET category = 'DECLARING_TYPE'
WHERE category = 'UNSPECIFIED'
  AND UPPER(name) IN ('CLASS', 'INTERFACE', 'ABSTRACT_CLASS', 'ABSTRACT CLASS', 'ENUM', 'RECORD', 'ANNOTATION');

UPDATE master_data
SET category = 'RELATION_TYPE'
WHERE category = 'UNSPECIFIED'
  AND (
    UPPER(name) LIKE '%INHERIT%'
    OR UPPER(name) LIKE '%GENERAL%'
    OR UPPER(name) LIKE '%COMPOS%'
    OR UPPER(name) LIKE '%AGGREG%'
    OR UPPER(name) LIKE '%ASSOCI%'
    OR UPPER(name) LIKE '%DEPEND%'
    OR UPPER(name) LIKE '%REALIZ%'
    OR UPPER(name) LIKE '%IMPLEMENT%'
    OR UPPER(name) LIKE '%LINK%'
    OR UPPER(name) LIKE '%EXTEND%'
  );
