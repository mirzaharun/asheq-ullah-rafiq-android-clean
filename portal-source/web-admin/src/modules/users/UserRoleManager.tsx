import { useEffect, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Edit3,
  Plus,
  ShieldAlert,
  Trash2,
} from 'lucide-react';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { Modal } from '../../components/common/Modal';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  mapAdminRecord,
  mapCitizenUserRecord,
  removeAdminRoleAssignment,
  saveAdminRoleAssignment,
  subscribeCollection,
} from '../../services/firebase/crudService';
import type { AdminRole } from '../../types/auth';
import type { AdminUserRecord, CitizenUserRecord } from '../../types/firestore';
import { formatFirestoreTimestamp, toLocalizedNumber } from '../../utils/formatters';

export function UserRoleManager() {
  const { admin } = useAuth();
  const { lang, t } = useLanguage();

  const [admins, setAdmins] = useState<AdminUserRecord[]>([]);
  const [citizens, setCitizens] = useState<CitizenUserRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [isNew, setIsNew] = useState(true);
  const [targetUid, setTargetUid] = useState('');
  const [email, setEmail] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [role, setRole] = useState<AdminRole>('editor');
  const [active, setActive] = useState(true);
  const [saving, setSaving] = useState(false);

  const [removeTarget, setRemoveTarget] = useState<AdminUserRecord | null>(null);
  const [removing, setRemoving] = useState(false);

  useEffect(() => {
    if (admin?.role !== 'super_admin') {
      setLoading(false);
      return;
    }

    const unsubAdmins = subscribeCollection(
      'admins',
      mapAdminRecord,
      (items) => {
        setAdmins(items);
        setLoading(false);
        setLoadError(null);
      },
      (msg) => {
        setLoadError(msg);
        setLoading(false);
      }
    );

    const unsubUsers = subscribeCollection(
      'users',
      mapCitizenUserRecord,
      (items) => setCitizens(items),
      () => {
        // Citizen list failure handled gracefully without overriding admin table
      }
    );

    return () => {
      unsubAdmins();
      unsubUsers();
    };
  }, [admin?.role]);

  if (admin?.role !== 'super_admin') {
    return (
      <div className="max-w-xl bg-white border border-slate-200 rounded-xl p-6">
        <div className="flex items-center gap-2.5 text-amber-700 font-semibold">
          <ShieldAlert className="w-5 h-5" />
          <span>
            {lang === 'bn'
              ? 'শুধুমাত্র সুপার অ্যাডমিনের জন্য সংরক্ষিত'
              : 'Restricted to Super Admin'}
          </span>
        </div>
      </div>
    );
  }

  const openCreate = () => {
    setIsNew(true);
    setTargetUid('');
    setEmail('');
    setDisplayName('');
    setRole('editor');
    setActive(true);
    setFeedback(null);
    setModalOpen(true);
  };

  const openEdit = (item: AdminUserRecord) => {
    setIsNew(false);
    setTargetUid(item.uid);
    setEmail(item.email);
    setDisplayName(item.displayName ?? '');
    setRole(item.role);
    setActive(item.active);
    setFeedback(null);
    setModalOpen(true);
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin) return;

    if (!/^[a-zA-Z0-9_-]{6,128}$/.test(targetUid.trim())) {
      setFeedback({
        type: 'error',
        text:
          lang === 'bn'
            ? 'বৈধ Firebase Authentication UID লিখুন (ইমেইল দিয়ে নয়)।'
            : 'Please enter a valid verified Firebase Authentication UID.',
      });
      return;
    }

    setSaving(true);
    setFeedback(null);
    try {
      await saveAdminRoleAssignment({
        targetUid,
        email,
        displayName,
        role,
        active,
        isNew,
        actor: admin,
      });
      setModalOpen(false);
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? 'অ্যাডমিন ভূমিকা সফলভাবে সংরক্ষিত হয়েছে।'
            : 'Administrator role assignment saved.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to update role.',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleConfirmRemove = async () => {
    if (!admin || !removeTarget) return;
    setRemoving(true);
    try {
      await removeAdminRoleAssignment(removeTarget.uid, admin);
      setRemoveTarget(null);
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? 'অ্যাডমিন অনুমোদন প্রত্যাহার করা হয়েছে।'
            : 'Administrator access removed.',
      });
    } catch (err) {
      setRemoveTarget(null);
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to remove admin.',
      });
    } finally {
      setRemoving(false);
    }
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 font-display">
            {lang === 'bn' ? 'ব্যবহারকারী ও ভূমিকা ব্যবস্থাপনা' : 'User & Role Management'}
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            admins · {toLocalizedNumber(admins.length, lang)}{' '}
            {lang === 'bn' ? 'জন অনুমোদিত অ্যাডমিন' : 'authorized admins'}
          </p>
        </div>

        <button
          type="button"
          onClick={openCreate}
          className="inline-flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>{lang === 'bn' ? 'অ্যাডমিন ভূমিকা নির্ধারণ' : 'Assign Admin Role'}</span>
        </button>
      </div>

      <div className="bg-blue-50/70 border border-blue-200 rounded-xl p-4 text-xs text-slate-700 leading-relaxed">
        {lang === 'bn'
          ? 'নিরাপত্তা নীতি: এই ইন্টারফেস শুধুমাত্র পূর্বে তৈরি Firebase Authentication UID-এর বিপরীতে ভূমিকা নির্ধারণ করে। কোনো নতুন পাসওয়ার্ড বা স্বয়ংক্রিয় অ্যাকাউন্ট এখানে তৈরি করা হয় না, এবং কেবল ইমেইলের ভিত্তিতে কাউকে অনুমোদন দেওয়া হয় না।'
          : 'Security Policy: Role assignments strictly bind to existing verified Firebase Authentication UIDs. No Auth accounts are created here, and privileges are never granted by email alone.'}
      </div>

      {feedback ? (
        <div
          role="alert"
          className={`p-3.5 rounded-xl border flex items-start gap-2.5 text-sm ${
            feedback.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-red-50 border-red-200 text-red-800'
          }`}
        >
          {feedback.type === 'success' ? (
            <CheckCircle2 className="w-4 h-4 text-emerald-700 shrink-0 mt-0.5" />
          ) : (
            <AlertCircle className="w-4 h-4 text-red-700 shrink-0 mt-0.5" />
          )}
          <span>{feedback.text}</span>
        </div>
      ) : null}

      {loadError ? (
        <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-sm text-red-800">
          {loadError}
        </div>
      ) : null}

      {/* Admin Roles Table */}
      <section className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-200">
          <h3 className="text-base font-semibold text-slate-900">
            {lang === 'bn' ? 'প্রশাসনিক ভূমিকা তালিকা (admins)' : 'Administrator Roles (admins)'}
          </h3>
        </div>

        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">
            {lang === 'bn' ? 'লোড হচ্ছে...' : 'Loading admins...'}
          </div>
        ) : admins.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500">
            {lang === 'bn'
              ? 'কোনো অ্যাডমিন রোল নথি পাওয়া যায়নি।'
              : 'No administrator role records found.'}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-600">
                  <th className="py-3 px-4">UID</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'ইমেইল ও নাম' : 'Email & Name'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'ভূমিকা' : 'Role'}</th>
                  <th className="py-3 px-4">{lang === 'bn' ? 'অবস্থা' : 'Status'}</th>
                  <th className="py-3 px-4 text-right">{lang === 'bn' ? 'কার্যক্রম' : 'Actions'}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {admins.map((a) => {
                  const isSelf = a.uid === admin.uid;
                  return (
                    <tr key={a.uid} className="hover:bg-slate-50/80">
                      <td className="py-3 px-4 font-mono text-xs text-slate-600">{a.uid}</td>
                      <td className="py-3 px-4">
                        <div className="font-medium text-slate-900">{a.email}</div>
                        <div className="text-xs text-slate-500">{a.displayName || '—'}</div>
                      </td>
                      <td className="py-3 px-4 text-xs font-semibold text-blue-700">
                        {t.roleLabels[a.role]}
                      </td>
                      <td className="py-3 px-4 text-xs font-medium">
                        <span className={a.active ? 'text-emerald-700' : 'text-red-700'}>
                          {a.active
                            ? lang === 'bn'
                              ? 'সক্রিয়'
                              : 'Active'
                            : lang === 'bn'
                            ? 'নিষ্ক্রিয়'
                            : 'Inactive'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right whitespace-nowrap">
                        <div className="inline-flex items-center gap-1.5">
                          <button
                            type="button"
                            onClick={() => openEdit(a)}
                            className="p-1.5 text-slate-600 hover:text-blue-700 hover:bg-blue-50 rounded-lg cursor-pointer"
                            aria-label="Edit admin role"
                          >
                            <Edit3 className="w-4 h-4" />
                          </button>
                          {!isSelf ? (
                            <button
                              type="button"
                              onClick={() => setRemoveTarget(a)}
                              className="p-1.5 text-slate-600 hover:text-red-700 hover:bg-red-50 rounded-lg cursor-pointer"
                              aria-label="Remove admin role"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Registered Citizens Read-only Summary */}
      <section className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between">
          <h3 className="text-base font-semibold text-slate-900">
            {lang === 'bn' ? 'নিবন্ধিত নাগরিক তালিকা (users)' : 'Registered Citizens (users)'}
          </h3>
          <span className="text-xs font-mono text-slate-500 tabular-nums">
            {toLocalizedNumber(citizens.length, lang)}
          </span>
        </div>
        {citizens.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500">
            {lang === 'bn'
              ? 'users সংগ্রহে বর্তমানে কোনো নিবন্ধিত নাগরিক নেই (০টি ডকুমেন্ট)।'
              : 'No citizen profiles currently exist in users (0 documents).'}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold text-slate-600">
                  <th className="py-2.5 px-4">UID</th>
                  <th className="py-2.5 px-4">{lang === 'bn' ? 'নাম' : 'Name'}</th>
                  <th className="py-2.5 px-4">{lang === 'bn' ? 'ইউনিয়ন · উপজেলা' : 'Union · Upazila'}</th>
                  <th className="py-2.5 px-4">{lang === 'bn' ? 'নিবন্ধন' : 'Joined'}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {citizens.map((c) => (
                  <tr key={c.id}>
                    <td className="py-2.5 px-4 font-mono text-xs text-slate-600">{c.uid || c.id}</td>
                    <td className="py-2.5 px-4 text-slate-900">{c.displayName || '—'}</td>
                    <td className="py-2.5 px-4 text-xs text-slate-600">
                      {[c.union, c.upazila].filter(Boolean).join(' · ') || '—'}
                    </td>
                    <td className="py-2.5 px-4 text-xs text-slate-500 tabular-nums">
                      {formatFirestoreTimestamp(c.createdAt, lang)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={
          isNew
            ? lang === 'bn'
              ? 'নতুন অ্যাডমিন ভূমিকা নির্ধারণ'
              : 'Assign Administrator Role'
            : lang === 'bn'
            ? 'অ্যাডমিন ভূমিকা হালনাগাদ'
            : 'Update Administrator Role'
        }
      >
        <form onSubmit={handleSave} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              {lang === 'bn'
                ? 'যাচাইকৃত Firebase Auth UID *'
                : 'Verified Firebase Auth UID *'}
            </label>
            <input
              type="text"
              required
              disabled={!isNew}
              value={targetUid}
              onChange={(e) => setTargetUid(e.target.value)}
              placeholder="e.g. a1B2c3D4..."
              className="w-full px-3 py-2 text-sm font-mono border border-slate-300 rounded-lg disabled:bg-slate-100"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'অ্যাডমিন ইমেইল *' : 'Email *'}
              </label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'প্রদর্শিত নাম' : 'Display Name'}
              </label>
              <input
                type="text"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'ভূমিকা (Role) *' : 'Role *'}
              </label>
              <select
                disabled={targetUid === admin.uid}
                value={role}
                onChange={(e) => setRole(e.target.value as AdminRole)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white disabled:bg-slate-100"
              >
                <option value="super_admin">{t.roleLabels.super_admin} (super_admin)</option>
                <option value="editor">{t.roleLabels.editor} (editor)</option>
                <option value="application_manager">
                  {t.roleLabels.application_manager} (application_manager)
                </option>
              </select>
            </div>
            <div className="flex items-end pb-2">
              <label className="inline-flex items-center gap-2 text-sm font-medium text-slate-800 cursor-pointer">
                <input
                  type="checkbox"
                  disabled={targetUid === admin.uid}
                  checked={active}
                  onChange={(e) => setActive(e.target.checked)}
                  className="w-4 h-4 rounded border-slate-300 text-emerald-700"
                />
                <span>{lang === 'bn' ? 'সক্রিয় (Active)' : 'Active'}</span>
              </label>
            </div>
          </div>

          <div className="pt-4 border-t border-slate-200 flex justify-end gap-3">
            <button
              type="button"
              onClick={() => setModalOpen(false)}
              className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 cursor-pointer"
            >
              {lang === 'bn' ? 'বাতিল' : 'Cancel'}
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-5 py-2 text-sm font-semibold text-white bg-blue-700 hover:bg-blue-800 rounded-lg disabled:opacity-60 cursor-pointer"
            >
              {saving
                ? lang === 'bn'
                  ? 'সংরক্ষণ হচ্ছে...'
                  : 'Saving...'
                : lang === 'bn'
                ? 'সংরক্ষণ করুন'
                : 'Save Assignment'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(removeTarget)}
        title={lang === 'bn' ? 'অ্যাডমিন ভূমিকা প্রত্যাহার' : 'Revoke Administrator Access'}
        message={
          lang === 'bn'
            ? `আপনি কি নিশ্চিতভাবে "${removeTarget?.email || removeTarget?.uid}" এর অ্যাডমিন অনুমোদন প্রত্যাহার করতে চান?`
            : `Revoke administrator privileges for "${removeTarget?.email || removeTarget?.uid}"?`
        }
        loading={removing}
        onConfirm={handleConfirmRemove}
        onCancel={() => setRemoveTarget(null)}
      />
    </div>
  );
}
