import React from 'react';
import { useDesktopPracticeBundleVersion } from '../../hooks/useDesktopPracticeBundleVersion';
import StudentOfflinePracticeDownload from './StudentOfflinePracticeDownload';
import './StudentPracticeBundleDownloadRow.css';

/** Web NavBar trailing: latest cloud bundle label + download pill. */
export default function StudentPracticeBundleDownloadRow({ onToast }) {
  const meta = useDesktopPracticeBundleVersion();

  return (
    <div className="practice-bundle-download-row">
      {meta?.version ? (
        <p className="practice-bundle-download-row__notice">
          Latest desktop version{' '}
          <span className="practice-bundle-download-row__emphasis">{meta.version}</span>
          {meta.updatedOn ? (
            <>
              {' '}
              updated on{' '}
              <span className="practice-bundle-download-row__emphasis">{meta.updatedOn}</span>
            </>
          ) : null}
          .
        </p>
      ) : null}
      <StudentOfflinePracticeDownload onToast={onToast} />
    </div>
  );
}