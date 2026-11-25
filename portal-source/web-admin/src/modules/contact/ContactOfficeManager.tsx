import { useEffect, useState, type FormEvent } from 'react';
import {
  AlertCircle,
  Building,
  CheckCircle2,
  Clock,
  MapPin,
  Phone,
  Plus,
  Save,
  Share2,
  Trash2,
  User,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useLanguage } from '../../hooks/useLanguage';
import {
  fetchDocSafe,
  saveGenericEntity,
  subscribeDoc,
} from '../../services/firebase/crudService';

interface ContactPerson {
  id: string;
  nameBn: string;
  nameEn: string;
  designationBn: string;
  phone: string;
}

interface OfficeContactData {
  id: string;
  officeNameBn: string;
  officeNameEn: string;
  addressBn: string;
  addressEn: string;
  phonePrimary: string;
  phoneSecondary: string;
  hotline: string;
  email: string;
  officeHoursBn: string;
  officeHoursEn: string;
  appointmentInstructionsBn: string;
  appointmentInstructionsEn: string;
  whatsappNumber: string;
  facebookPageUrl: string;
  youtubeUrl: string;
  googleMapsUrl: string;
  showOfficeHours: boolean;
  showWhatsApp: boolean;
  showDirections: boolean;
  showCallButton: boolean;
  showAppointments: boolean;
  showInApp: boolean;
  contactPersons: ContactPerson[];
}

const DEFAULT_OFFICE_DATA: OfficeContactData = {
  id: 'primary',
  officeNameBn: 'মাননীয় সংসদ সদস্যের সংসদীয় কার্যালয়',
  officeNameEn: 'Parliamentary Office of Hon. Member of Parliament',
  addressBn: 'মহেশখালী পৌরসভা কার্যালয় সংলগ্ন, গোরকঘাটা, মহেশখালী, কক্সবাজার।',
  addressEn: "Adjacent to Maheshkhali Municipality Office, Gorakghata, Maheshkhali, Cox's Bazar.",
  phonePrimary: '+8801819000000',
  phoneSecondary: '+8801711000000',
  hotline: '333',
  email: 'mp.office@ashequllah.portal',
  officeHoursBn: 'রবিবার থেকে বৃহস্পতিবার: সকাল ৯:০০ টা – বিকাল ৫:০০ টা (শুক্রবার ও শনিবার বন্ধ)',
  officeHoursEn: 'Sunday to Thursday: 9:00 AM – 5:00 PM (Closed on Friday & Saturday)',
  appointmentInstructionsBn:
    'সাক্ষাৎকারের জন্য অ্যাপের "নাগরিক আবেদন" ট্যাবের মাধ্যমে কমপক্ষে ৩ দিন পূর্বে আবেদন করুন। জরুরি ক্ষেত্রে সরাসরি যোগাযোগ করা যাবে।',
  appointmentInstructionsEn:
    'For appointments, please submit an interview request via the Citizen Requests tab at least 3 days prior.',
  whatsappNumber: '+8801819000000',
  facebookPageUrl: 'https://facebook.com/AsheqUllahRafiq',
  youtubeUrl: 'https://youtube.com/@AsheqUllahRafiq',
  googleMapsUrl: 'https://maps.google.com/?q=Maheshkhali,CoxsBazar',
  showOfficeHours: true,
  showWhatsApp: true,
  showDirections: true,
  showCallButton: true,
  showAppointments: true,
  showInApp: true,
  contactPersons: [
    {
      id: 'p1',
      nameBn: 'মোঃ রফিকুল ইসলাম',
      nameEn: 'Md. Rafiqul Islam',
      designationBn: 'ব্যক্তিগত সহকারী (পিএস)',
      phone: '+8801819111222',
    },
    {
      id: 'p2',
      nameBn: 'আবদুল্লাহ আল মামুন',
      nameEn: 'Abdullah Al Mamun',
      designationBn: 'দপ্তর সচিব',
      phone: '+8801711333444',
    },
  ],
};

function mapOfficeContactRecord(raw: Record<string, unknown>): OfficeContactData {
  const personsRaw = Array.isArray(raw.contactPersons) ? raw.contactPersons : null;
  const contactPersons: ContactPerson[] = personsRaw
    ? personsRaw
        .filter((item): item is Record<string, unknown> => typeof item === 'object' && item !== null)
        .map((item, idx) => ({
          id: typeof item.id === 'string' && item.id ? item.id : `cp_${idx + 1}`,
          nameBn: typeof item.nameBn === 'string' ? item.nameBn : '',
          nameEn: typeof item.nameEn === 'string' ? item.nameEn : '',
          designationBn: typeof item.designationBn === 'string' ? item.designationBn : '',
          phone: typeof item.phone === 'string' ? item.phone : '',
        }))
    : DEFAULT_OFFICE_DATA.contactPersons;

  return {
    id: 'primary',
    officeNameBn:
      typeof raw.officeNameBn === 'string' ? raw.officeNameBn : DEFAULT_OFFICE_DATA.officeNameBn,
    officeNameEn:
      typeof raw.officeNameEn === 'string' ? raw.officeNameEn : DEFAULT_OFFICE_DATA.officeNameEn,
    addressBn: typeof raw.addressBn === 'string' ? raw.addressBn : DEFAULT_OFFICE_DATA.addressBn,
    addressEn: typeof raw.addressEn === 'string' ? raw.addressEn : DEFAULT_OFFICE_DATA.addressEn,
    phonePrimary:
      typeof raw.phonePrimary === 'string'
        ? raw.phonePrimary
        : typeof raw.primaryPhone === 'string'
        ? raw.primaryPhone
        : DEFAULT_OFFICE_DATA.phonePrimary,
    phoneSecondary:
      typeof raw.phoneSecondary === 'string'
        ? raw.phoneSecondary
        : typeof raw.secondaryPhone === 'string'
        ? raw.secondaryPhone
        : DEFAULT_OFFICE_DATA.phoneSecondary,
    hotline: typeof raw.hotline === 'string' ? raw.hotline : DEFAULT_OFFICE_DATA.hotline,
    email: typeof raw.email === 'string' ? raw.email : DEFAULT_OFFICE_DATA.email,
    officeHoursBn:
      typeof raw.officeHoursBn === 'string'
        ? raw.officeHoursBn
        : DEFAULT_OFFICE_DATA.officeHoursBn,
    officeHoursEn:
      typeof raw.officeHoursEn === 'string'
        ? raw.officeHoursEn
        : DEFAULT_OFFICE_DATA.officeHoursEn,
    appointmentInstructionsBn:
      typeof raw.appointmentInstructionsBn === 'string'
        ? raw.appointmentInstructionsBn
        : typeof raw.appointmentInfoBn === 'string'
        ? raw.appointmentInfoBn
        : DEFAULT_OFFICE_DATA.appointmentInstructionsBn,
    appointmentInstructionsEn:
      typeof raw.appointmentInstructionsEn === 'string'
        ? raw.appointmentInstructionsEn
        : typeof raw.appointmentInfoEn === 'string'
        ? raw.appointmentInfoEn
        : DEFAULT_OFFICE_DATA.appointmentInstructionsEn,
    whatsappNumber:
      typeof raw.whatsappNumber === 'string'
        ? raw.whatsappNumber
        : DEFAULT_OFFICE_DATA.whatsappNumber,
    facebookPageUrl:
      typeof raw.facebookPageUrl === 'string'
        ? raw.facebookPageUrl
        : typeof raw.facebookUrl === 'string'
        ? raw.facebookUrl
        : DEFAULT_OFFICE_DATA.facebookPageUrl,
    youtubeUrl:
      typeof raw.youtubeUrl === 'string' ? raw.youtubeUrl : DEFAULT_OFFICE_DATA.youtubeUrl,
    googleMapsUrl:
      typeof raw.googleMapsUrl === 'string'
        ? raw.googleMapsUrl
        : DEFAULT_OFFICE_DATA.googleMapsUrl,
    showOfficeHours:
      typeof raw.showOfficeHours === 'boolean'
        ? raw.showOfficeHours
        : DEFAULT_OFFICE_DATA.showOfficeHours,
    showWhatsApp:
      typeof raw.showWhatsApp === 'boolean'
        ? raw.showWhatsApp
        : DEFAULT_OFFICE_DATA.showWhatsApp,
    showDirections:
      typeof raw.showDirections === 'boolean'
        ? raw.showDirections
        : DEFAULT_OFFICE_DATA.showDirections,
    showCallButton:
      typeof raw.showCallButton === 'boolean'
        ? raw.showCallButton
        : DEFAULT_OFFICE_DATA.showCallButton,
    showAppointments:
      typeof raw.showAppointments === 'boolean'
        ? raw.showAppointments
        : DEFAULT_OFFICE_DATA.showAppointments,
    showInApp:
      typeof raw.showInApp === 'boolean' ? raw.showInApp : DEFAULT_OFFICE_DATA.showInApp,
    contactPersons,
  };
}

export function ContactOfficeManager() {
  const { admin } = useAuth();
  const { lang } = useLanguage();

  const [form, setForm] = useState<OfficeContactData>(DEFAULT_OFFICE_DATA);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    let active = true;
    const unsub = subscribeDoc(
      'office_contact',
      'primary',
      async (data) => {
        if (!active) return;
        if (data) {
          setForm(mapOfficeContactRecord(data));
          setLoading(false);
          return;
        }
        // Seamless fallback migration if data was previously saved under config/office_contact
        const legacyData = await fetchDocSafe('config', 'office_contact');
        if (!active) return;
        if (legacyData) {
          setForm(mapOfficeContactRecord(legacyData));
        }
        setLoading(false);
      },
      () => {
        if (active) setLoading(false);
      }
    );

    return () => {
      active = false;
      unsub();
    };
  }, []);

  const canEdit = admin?.role === 'super_admin' || admin?.role === 'editor';

  const handleAddPerson = () => {
    setForm((prev) => ({
      ...prev,
      contactPersons: [
        ...prev.contactPersons,
        {
          id: `cp_${Date.now()}`,
          nameBn: '',
          nameEn: '',
          designationBn: '',
          phone: '',
        },
      ],
    }));
  };

  const handleRemovePerson = (id: string) => {
    setForm((prev) => ({
      ...prev,
      contactPersons: prev.contactPersons.filter((p) => p.id !== id),
    }));
  };

  const handlePersonChange = (id: string, field: keyof ContactPerson, value: string) => {
    setForm((prev) => ({
      ...prev,
      contactPersons: prev.contactPersons.map((p) => (p.id === id ? { ...p, [field]: value } : p)),
    }));
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!admin || !canEdit) return;

    setSaving(true);
    setFeedback(null);
    try {
      await saveGenericEntity(
        'office_contact',
        'primary',
        {
          ...form,
          id: 'primary',
          updatedAt: new Date().toISOString(),
          updatedByUid: admin.uid,
        } as unknown as Record<string, unknown>,
        false,
        admin,
        'অফিস ও যোগাযোগ ব্যবস্থাপনা'
      );
      setFeedback({
        type: 'success',
        text:
          lang === 'bn'
            ? 'অফিস ও যোগাযোগ সংক্রান্ত তথ্য সফলভাবে office_contact/primary-তে সংরক্ষিত হয়েছে এবং মোবাইল অ্যাপে সিঙ্ক করা হয়েছে।'
            : 'Office and contact information successfully saved to office_contact/primary and synced to mobile app.',
      });
    } catch (err) {
      setFeedback({
        type: 'error',
        text: err instanceof Error ? err.message : 'Failed to save contact info.',
      });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="p-8 text-center text-sm text-slate-500">
        <span className="w-5 h-5 border-2 border-emerald-700 border-t-transparent rounded-full inline-block animate-spin mr-2" />
        {lang === 'bn' ? 'যোগাযোগ তথ্য লোড হচ্ছে...' : 'Loading contact info...'}
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 font-display flex items-center gap-2">
            <Building className="w-6 h-6 text-[#006A4E]" />
            <span>
              {lang === 'bn' ? 'অফিস ও যোগাযোগ ব্যবস্থাপনা CMS' : 'Contact & Office Management CMS'}
            </span>
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            {lang === 'bn'
              ? 'মোবাইল অ্যাপের "যোগাযোগ ও সংসদীয় কার্যালয়" পেজের (office_contact/primary) সকল তথ্য, হটলাইন, গুগল ম্যাপ ও বোতাম রিয়েল-টাইমে নিয়ন্ত্রণ করুন।'
              : 'Centrally manage Parliamentary office address, hotlines, WhatsApp, Google Maps, and visibility toggles in real time (office_contact/primary).'}
          </p>
        </div>
      </div>

      {feedback && (
        <div
          role="alert"
          className={`p-4 rounded-xl border flex items-start gap-3 text-sm ${
            feedback.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-red-50 border-red-200 text-red-800'
          }`}
        >
          {feedback.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-700 shrink-0 mt-0.5" />
          ) : (
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
          )}
          <span>{feedback.text}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-8">
        {/* Section 1: Office General Info */}
        <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs space-y-4">
          <h3 className="text-base font-semibold text-slate-900 border-b border-slate-100 pb-3 flex items-center gap-2">
            <MapPin className="w-4 h-4 text-[#006A4E]" />
            <span>{lang === 'bn' ? 'কার্যালয়ের নাম ও ঠিকানা' : 'Office Name & Location'}</span>
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'কার্যালয়ের নাম (বাংলা)' : 'Office Name (Bengali)'}
              </label>
              <input
                type="text"
                required
                value={form.officeNameBn}
                onChange={(e) => setForm({ ...form, officeNameBn: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900 focus:ring-2 focus:ring-[#006A4E]/20 focus:border-[#006A4E]"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'কার্যালয়ের নাম (ইংরেজি)' : 'Office Name (English)'}
              </label>
              <input
                type="text"
                value={form.officeNameEn}
                onChange={(e) => setForm({ ...form, officeNameEn: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900 focus:ring-2 focus:ring-[#006A4E]/20 focus:border-[#006A4E]"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'পূর্ণাঙ্গ ঠিকানা (বাংলা)' : 'Address (Bengali)'}
              </label>
              <textarea
                rows={2}
                value={form.addressBn}
                onChange={(e) => setForm({ ...form, addressBn: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900 focus:ring-2 focus:ring-[#006A4E]/20 focus:border-[#006A4E]"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'পূর্ণাঙ্গ ঠিকানা (ইংরেজি)' : 'Address (English)'}
              </label>
              <textarea
                rows={2}
                value={form.addressEn}
                onChange={(e) => setForm({ ...form, addressEn: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900 focus:ring-2 focus:ring-[#006A4E]/20 focus:border-[#006A4E]"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              {lang === 'bn' ? 'গুগল ম্যাপ লিংক (Google Maps URL)' : 'Google Maps Directions URL'}
            </label>
            <input
              type="text"
              value={form.googleMapsUrl}
              onChange={(e) => setForm({ ...form, googleMapsUrl: e.target.value })}
              placeholder="https://maps.google.com/?q=..."
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900 focus:ring-2 focus:ring-[#006A4E]/20 focus:border-[#006A4E]"
            />
          </div>
        </div>

        {/* Section 2: Contact Hotlines */}
        <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs space-y-4">
          <h3 className="text-base font-semibold text-slate-900 border-b border-slate-100 pb-3 flex items-center gap-2">
            <Phone className="w-4 h-4 text-[#006A4E]" />
            <span>
              {lang === 'bn' ? 'হটলাইন, মোবাইল ও ডিজিটাল চ্যানেল' : 'Hotlines, Phone & Digital Channels'}
            </span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'প্রধান ফোন নম্বর' : 'Primary Phone'}
              </label>
              <input
                type="text"
                value={form.phonePrimary}
                onChange={(e) => setForm({ ...form, phonePrimary: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'বিকল্প ফোন নম্বর' : 'Secondary Phone'}
              </label>
              <input
                type="text"
                value={form.phoneSecondary}
                onChange={(e) => setForm({ ...form, phoneSecondary: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'জরুরি হেল্পলাইন নম্বর' : 'Hotline Code'}
              </label>
              <input
                type="text"
                value={form.hotline}
                onChange={(e) => setForm({ ...form, hotline: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'অফিসিয়াল ইমেইল' : 'Email Address'}
              </label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'হোয়াটসঅ্যাপ নম্বর (WhatsApp)' : 'WhatsApp Number'}
              </label>
              <input
                type="text"
                value={form.whatsappNumber}
                onChange={(e) => setForm({ ...form, whatsappNumber: e.target.value })}
                placeholder="+8801819..."
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'ফেসবুক পেজ লিংক' : 'Facebook Page URL'}
              </label>
              <input
                type="text"
                value={form.facebookPageUrl}
                onChange={(e) => setForm({ ...form, facebookPageUrl: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900"
              />
            </div>
          </div>
        </div>

        {/* Section 3: Office Hours & Appointments */}
        <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs space-y-4">
          <h3 className="text-base font-semibold text-slate-900 border-b border-slate-100 pb-3 flex items-center gap-2">
            <Clock className="w-4 h-4 text-[#006A4E]" />
            <span>
              {lang === 'bn' ? 'অফিস সময় ও সাক্ষাৎকার নির্দেশিকা' : 'Office Hours & Appointments'}
            </span>
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'অফিস সময়সূচি (বাংলা)' : 'Office Hours (Bengali)'}
              </label>
              <textarea
                rows={2}
                value={form.officeHoursBn}
                onChange={(e) => setForm({ ...form, officeHoursBn: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'অফিস সময়সূচি (ইংরেজি)' : 'Office Hours (English)'}
              </label>
              <textarea
                rows={2}
                value={form.officeHoursEn}
                onChange={(e) => setForm({ ...form, officeHoursEn: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn' ? 'সাক্ষাৎকার গ্রহণের নির্দেশনা (বাংলা)' : 'Appointment Guidance (Bengali)'}
              </label>
              <textarea
                rows={2}
                value={form.appointmentInstructionsBn}
                onChange={(e) => setForm({ ...form, appointmentInstructionsBn: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {lang === 'bn'
                  ? 'সাক্ষাৎকার গ্রহণের নির্দেশনা (ইংরেজি)'
                  : 'Appointment Guidance (English)'}
              </label>
              <textarea
                rows={2}
                value={form.appointmentInstructionsEn}
                onChange={(e) => setForm({ ...form, appointmentInstructionsEn: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg text-slate-900"
              />
            </div>
          </div>
        </div>

        {/* Section 4: Key Contact Persons */}
        <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <h3 className="text-base font-semibold text-slate-900 flex items-center gap-2">
              <User className="w-4 h-4 text-[#006A4E]" />
              <span>
                {lang === 'bn' ? 'দায়িত্বপ্রাপ্ত ব্যক্তি ও কর্মকর্তা' : 'Key Contact Persons'}
              </span>
            </h3>
            {canEdit && (
              <button
                type="button"
                onClick={handleAddPerson}
                className="px-3 py-1.5 text-xs font-semibold text-[#006A4E] bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-lg transition-colors flex items-center gap-1 cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>{lang === 'bn' ? 'নতুন কর্মকর্তা যোগ' : 'Add Person'}</span>
              </button>
            )}
          </div>

          <div className="space-y-3">
            {form.contactPersons.map((p) => (
              <div
                key={p.id}
                className="p-3.5 bg-slate-50 border border-slate-200 rounded-lg grid grid-cols-1 sm:grid-cols-4 gap-3 items-end"
              >
                <div>
                  <label className="block text-[11px] font-medium text-slate-600 mb-1">
                    {lang === 'bn' ? 'কর্মকর্তার নাম (বাংলা)' : 'Name (Bengali)'}
                  </label>
                  <input
                    type="text"
                    value={p.nameBn}
                    onChange={(e) => handlePersonChange(p.id, 'nameBn', e.target.value)}
                    placeholder="উদা: মোঃ রফিকুল ইসলাম"
                    className="w-full px-2.5 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-medium text-slate-600 mb-1">
                    {lang === 'bn' ? 'পদবী (উদা: পিএস / সচিব)' : 'Designation'}
                  </label>
                  <input
                    type="text"
                    value={p.designationBn}
                    onChange={(e) => handlePersonChange(p.id, 'designationBn', e.target.value)}
                    className="w-full px-2.5 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-medium text-slate-600 mb-1">
                    {lang === 'bn' ? 'যোগাযোগ নম্বর' : 'Phone Number'}
                  </label>
                  <input
                    type="text"
                    value={p.phone}
                    onChange={(e) => handlePersonChange(p.id, 'phone', e.target.value)}
                    className="w-full px-2.5 py-1.5 text-xs bg-white border border-slate-300 rounded text-slate-900"
                  />
                </div>
                <div className="flex justify-end">
                  {canEdit && (
                    <button
                      type="button"
                      onClick={() => handleRemovePerson(p.id)}
                      className="p-1.5 text-red-600 hover:bg-red-50 rounded transition-colors cursor-pointer"
                      title={lang === 'bn' ? 'মুছে ফেলুন' : 'Remove'}
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Section 5: Show / Hide Section Toggles */}
        <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs space-y-4">
          <h3 className="text-base font-semibold text-slate-900 border-b border-slate-100 pb-3 flex items-center gap-2">
            <Share2 className="w-4 h-4 text-[#006A4E]" />
            <span>
              {lang === 'bn'
                ? 'মোবাইল অ্যাপ প্রদর্শন সুইচ (Show / Hide Toggles)'
                : 'App Section Visibility'}
            </span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {(
              [
                {
                  key: 'showInApp',
                  labelBn: 'অ্যাপে যোগাযোগ সেকশন সক্রিয় রাখুন',
                  labelEn: 'Show Contact Section in App',
                },
                {
                  key: 'showCallButton',
                  labelBn: '"সরাসরি কল করুন" বোতাম',
                  labelEn: '"Call Now" Action Button',
                },
                {
                  key: 'showWhatsApp',
                  labelBn: '"হোয়াটসঅ্যাপ চ্যাট" বোতাম',
                  labelEn: '"WhatsApp Chat" Button',
                },
                {
                  key: 'showDirections',
                  labelBn: '"ম্যাপে রুট দেখুন" বোতাম',
                  labelEn: '"Get Directions" Button',
                },
                {
                  key: 'showOfficeHours',
                  labelBn: 'অফিস সময়সূচি প্রদর্শন',
                  labelEn: 'Show Office Hours',
                },
                {
                  key: 'showAppointments',
                  labelBn: 'সাক্ষাৎকার নির্দেশিকা প্রদর্শন',
                  labelEn: 'Show Appointment Guide',
                },
              ] as const
            ).map((toggle) => (
              <label
                key={toggle.key}
                className="flex items-center gap-2.5 p-3 rounded-lg border border-slate-200 bg-slate-50/50 hover:bg-slate-50 cursor-pointer"
              >
                <input
                  type="checkbox"
                  checked={Boolean(form[toggle.key])}
                  onChange={(e) => setForm({ ...form, [toggle.key]: e.target.checked })}
                  className="w-4 h-4 rounded text-[#006A4E] focus:ring-[#006A4E]"
                />
                <span className="text-xs font-medium text-slate-800">
                  {lang === 'bn' ? toggle.labelBn : toggle.labelEn}
                </span>
              </label>
            ))}
          </div>
        </div>

        {canEdit && (
          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={saving}
              className="px-6 py-2.5 text-sm font-semibold text-white bg-[#006A4E] hover:bg-[#00553e] disabled:opacity-60 rounded-lg transition-colors flex items-center gap-2 shadow-sm cursor-pointer"
            >
              <Save className="w-4 h-4" />
              <span>
                {saving
                  ? lang === 'bn'
                    ? 'সংরক্ষণ হচ্ছে...'
                    : 'Saving...'
                  : lang === 'bn'
                  ? 'পরিবর্তনসমূহ সংরক্ষণ করুন'
                  : 'Save All Changes'}
              </span>
            </button>
          </div>
        )}
      </form>
    </div>
  );
}
