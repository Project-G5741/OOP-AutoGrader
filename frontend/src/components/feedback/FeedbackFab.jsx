import { useState } from 'react';
import { MessageSquarePlus } from 'lucide-react';
import FeedbackForm from './FeedbackForm';

export default function FeedbackFab({ user }) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <button
        type="button"
        aria-label="Send feedback"
        onClick={() => setOpen(true)}
        className="fixed bottom-4 right-4 z-[90] inline-flex h-14 w-14 items-center justify-center rounded-full bg-primary text-white shadow-lg hover:bg-primary-hover"
      >
        <MessageSquarePlus className="h-6 w-6" />
      </button>
      {open && <FeedbackForm user={user} onClose={() => setOpen(false)} />}
    </>
  );
}
