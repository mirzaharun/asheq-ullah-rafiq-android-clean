import { AlertTriangle } from 'lucide-react';
import { Modal } from './Modal';
import { useLanguage } from '../../hooks/useLanguage';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel,
  cancelLabel,
  destructive = true,
  loading = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const { lang } = useLanguage();

  return (
    <Modal
      open={open}
      title={title}
      onClose={onCancel}
      maxWidthClass="max-w-md"
      footer={
        <>
          <button
            type="button"
            onClick={onCancel}
            disabled={loading}
            className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors cursor-pointer"
          >
            {cancelLabel ?? (lang === 'bn' ? 'বাতিল' : 'Cancel')}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={loading}
            className={`px-4 py-2 text-sm font-semibold text-white rounded-lg transition-colors disabled:opacity-60 cursor-pointer ${
              destructive
                ? 'bg-red-700 hover:bg-red-800'
                : 'bg-blue-700 hover:bg-blue-800'
            }`}
          >
            {loading
              ? lang === 'bn'
                ? 'সম্পন্ন হচ্ছে...'
                : 'Processing...'
              : confirmLabel ?? (lang === 'bn' ? 'নিশ্চিত করুন' : 'Confirm')}
          </button>
        </>
      }
    >
      <div className="flex items-start gap-3">
        <div
          className={`w-9 h-9 rounded-lg flex items-center justify-center shrink-0 ${
            destructive ? 'bg-red-50 text-red-700' : 'bg-blue-50 text-blue-700'
          }`}
        >
          <AlertTriangle className="w-5 h-5" />
        </div>
        <p className="text-sm text-slate-700 leading-relaxed">{message}</p>
      </div>
    </Modal>
  );
}
