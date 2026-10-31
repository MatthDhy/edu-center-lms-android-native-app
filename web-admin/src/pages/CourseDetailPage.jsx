import React, { useState, useEffect, useCallback } from 'react';
import {
  ArrowLeft, Pencil, Trash2, Plus, Repeat, UserPlus, UserMinus, Eye, RefreshCw,
  Info, CalendarDays, Users, ClipboardList, FileText, ExternalLink
} from 'lucide-react';
import { courseApi, adminApi } from '../api/client';
import LessonFormModal from '../components/LessonFormModal';
import GenerateLessonsModal from '../components/GenerateLessonsModal';
import EnrollStudentsModal from '../components/EnrollStudentsModal';
import SubmissionsModal from '../components/SubmissionsModal';
import CourseFormModal from '../components/CourseFormModal';
import ConfirmModal from '../components/ConfirmModal';
import { WEEKDAYS, fmtDate, fmtDateTime, fmtMoney, fmtTime } from '../utils/format';
import { btnPrimary } from '../components/ui';
import MaterialFormModal from '../components/MaterialFormModal';

const TABS = [
  { id: 'info', label: 'Tổng quan', icon: Info },
  { id: 'lessons', label: 'Buổi học', icon: CalendarDays },
  { id: 'students', label: 'Học viên', icon: Users },
  { id: 'assignments', label: 'Bài tập', icon: ClipboardList },
  { id: 'materials', label: 'Tài liệu', icon: FileText },
];

const LESSON_STYLE = {
  SCHEDULED: 'bg-blue-50 text-blue-700 border-blue-200',
  DONE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  CANCELED: 'bg-rose-50 text-rose-700 border-rose-200',
};

function Empty({ text }) {
  return <div className="py-14 text-center text-sm text-slate-400">{text}</div>;
}

export default function CourseDetailPage({ courseId, teachers, onBack }) {
  const [course, setCourse] = useState(null);
  const [tab, setTab] = useState('info');
  const [error, setError] = useState('');

  const [lessons, setLessons] = useState([]);
  const [roster, setRoster] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [materials, setMaterials] = useState([]);
  const [materialFormOpen, setMaterialFormOpen] = useState(false);
  const [deleteMaterial, setDeleteMaterial] = useState(null);
  const [tabLoading, setTabLoading] = useState(false);

  const [courseFormOpen, setCourseFormOpen] = useState(false);
  const [lessonForm, setLessonForm] = useState({ open: false, lesson: null });
  const [genOpen, setGenOpen] = useState(false);
  const [enrollOpen, setEnrollOpen] = useState(false);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [removeTarget, setRemoveTarget] = useState(null);
  const [submissionsFor, setSubmissionsFor] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState('');
  const [teacherList, setTeacherList] = useState(teachers || []);

  useEffect(() => {
    if (teachers && teachers.length) return;
    adminApi.getUsers({ role: 'TEACHER', status: 'ACTIVE' }).then((r) => setTeacherList(r.data || [])).catch(() => {});
  }, [teachers]);

  const loadCourse = useCallback(async () => {
    try {
      const res = await courseApi.get(courseId);
      setCourse(res.data);
    } catch (err) {
      setError(err.message);
    }
  }, [courseId]);

  const loadTab = useCallback(async () => {
    setTabLoading(true);
    setError('');
    try {
      if (tab === 'lessons') setLessons((await courseApi.lessons(courseId)).data || []);
      if (tab === 'students') setRoster((await courseApi.roster(courseId)).data || []);
      if (tab === 'assignments') setAssignments((await courseApi.assignments(courseId)).data || []);
      if (tab === 'materials') setMaterials((await courseApi.materials(courseId)).data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setTabLoading(false);
    }
  }, [tab, courseId]);

  useEffect(() => { loadCourse(); }, [loadCourse]);
  useEffect(() => { loadTab(); }, [loadTab]);

  const refreshAll = () => { loadCourse(); loadTab(); };

  const doCancelLesson = async () => {
    setActionLoading(true);
    setActionError('');
    try {
      await courseApi.cancelLesson(cancelTarget.id);
      setCancelTarget(null);
      refreshAll();
    } catch (err) {
      setActionError(err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const doRemoveStudent = async () => {
    setActionLoading(true);
    setActionError('');
    try {
      await courseApi.removeStudent(courseId, removeTarget.studentId);
      setRemoveTarget(null);
      refreshAll();
    } catch (err) {
      setActionError(err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const doDeleteMaterial = async () => {
    setActionLoading(true); setActionError('');
    try {
      await courseApi.deleteMaterial(deleteMaterial.id);
      setDeleteMaterial(null);
      refreshAll();
    } catch (err) { setActionError(err.message); }
    finally { setActionLoading(false); }
  };

  if (!course) {
    return (
      <div className="space-y-4">
        <button onClick={onBack} className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-slate-900">
          <ArrowLeft className="w-4 h-4" /> Quay lại danh sách
        </button>
        {error ? <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-sm text-rose-700">{error}</div> : <Empty text="Đang tải..." />}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <button onClick={onBack} className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-slate-900 mb-2">
            <ArrowLeft className="w-4 h-4" /> Quay lại danh sách
          </button>
          <h3 className="text-xl font-bold text-slate-800">{course.courseName}</h3>
          <p className="text-xs text-slate-500 mt-0.5">Giảng viên: <strong>{course.teacherName}</strong> · {course.studentCount} học viên · {course.lessonCount} buổi đã xếp</p>
        </div>
        <button onClick={() => setCourseFormOpen(true)} className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-amber-200 text-amber-700 hover:bg-amber-50 font-semibold text-sm transition-colors">
          <Pencil className="w-4 h-4" /> Sửa thông tin lớp
        </button>
      </div>

      <div className="flex flex-wrap gap-2 border-b border-slate-200 pb-3">
        {TABS.map((t) => {
          const Icon = t.icon;
          return (
            <button
              key={t.id}
              onClick={() => setTab(t.id)}
              className={`inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold border transition-all ${
                tab === t.id ? 'bg-brand-600 border-brand-600 text-white shadow-md shadow-brand-600/20' : 'border-slate-200 text-slate-600 hover:bg-slate-50'
              }`}
            >
              <Icon className="w-4 h-4" /> {t.label}
            </button>
          );
        })}
      </div>

      {error && <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-sm text-rose-700">{error}</div>}

      {/* ===== Tổng quan ===== */}
      {tab === 'info' && (
        <div className="bg-white rounded-xl border border-slate-200 p-6 grid grid-cols-1 sm:grid-cols-2 gap-5 text-sm">
          {[
            ['Tên lớp', course.courseName],
            ['Giảng viên', course.teacherName],
            ['Học phí', fmtMoney(course.tuitionFee)],
            ['Tổng số buổi dự kiến', course.totalSessions ?? '—'],
            ['Đã xếp lịch', `${course.lessonCount} buổi`],
            ['Đã diễn ra', `${course.heldCount} buổi`],
            ['Sĩ số hiện tại', `${course.studentCount} học viên`],
            ['Ngày tạo', fmtDate(course.createdAt)],
            ['Trạng thái', course.status],
          ].map(([k, v]) => (
            <div key={k}>
              <p className="text-xs font-bold uppercase tracking-wider text-slate-400">{k}</p>
              <p className="font-semibold text-slate-800 mt-1">{v}</p>
            </div>
          ))}
          <div className="sm:col-span-2">
            <p className="text-xs font-bold uppercase tracking-wider text-slate-400">Mô tả</p>
            <p className="text-slate-700 mt-1 whitespace-pre-line">{course.description || '—'}</p>
          </div>
        </div>
      )}

      {/* ===== Buổi học ===== */}
      {tab === 'lessons' && (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-2 justify-between">
            <button onClick={loadTab} className="p-2.5 text-slate-500 hover:bg-slate-100 rounded-xl border border-slate-200" title="Làm mới">
              <RefreshCw className={`w-4 h-4 ${tabLoading ? 'animate-spin' : ''}`} />
            </button>
            <div className="flex gap-2">
              <button onClick={() => setGenOpen(true)} className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-brand-200 text-brand-700 hover:bg-brand-50 font-semibold text-sm">
                <Repeat className="w-4 h-4" /> Sinh lịch lặp
              </button>
              <button onClick={() => setLessonForm({ open: true, lesson: null })} className={btnPrimary}>
                <Plus className="w-4 h-4" /> Thêm buổi
              </button>
            </div>
          </div>
          {lessons.length === 0 ? (
            <div className="bg-white rounded-xl border border-slate-200"><Empty text="Lớp chưa có buổi học nào. Dùng “Sinh lịch lặp” để tạo nhanh cả khóa." /></div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-50 text-left text-xs font-bold text-slate-500 uppercase tracking-wider border-b border-slate-200">
                    <th className="px-5 py-3">#</th>
                    <th className="px-5 py-3">Ngày</th>
                    <th className="px-5 py-3">Giờ</th>
                    <th className="px-5 py-3">Phòng</th>
                    <th className="px-5 py-3">Chủ đề</th>
                    <th className="px-5 py-3">Trạng thái</th>
                    <th className="px-5 py-3 text-right">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {lessons.map((l, i) => {
                    const d = new Date(l.startTime);
                    return (
                      <tr key={l.id} className={l.status === 'CANCELED' ? 'opacity-60' : ''}>
                        <td className="px-5 py-3 text-slate-400">{i + 1}</td>
                        <td className="px-5 py-3 font-semibold text-slate-800">{WEEKDAYS[(d.getDay() + 6) % 7]}, {fmtDate(l.startTime)}</td>
                        <td className="px-5 py-3 text-slate-700">{fmtTime(l.startTime)} – {fmtTime(l.endTime)}</td>
                        <td className="px-5 py-3 text-slate-700">{l.room || '—'}</td>
                        <td className="px-5 py-3 text-slate-600">{l.topic || '—'}</td>
                        <td className="px-5 py-3">
                          <span className={`px-2 py-0.5 rounded-full text-[11px] font-bold border ${LESSON_STYLE[l.status]}`}>{l.status}</span>
                        </td>
                        <td className="px-5 py-3">
                          <div className="flex items-center justify-end gap-2">
                            <button onClick={() => setLessonForm({ open: true, lesson: l })} title="Sửa" className="p-2 rounded-lg text-amber-600 hover:bg-amber-50 border border-amber-200">
                              <Pencil className="w-4 h-4" />
                            </button>
                            {l.status !== 'CANCELED' && (
                              <button onClick={() => { setActionError(''); setCancelTarget(l); }} title="Hủy buổi" className="p-2 rounded-lg text-rose-600 hover:bg-rose-50 border border-rose-200">
                                <Trash2 className="w-4 h-4" />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ===== Học viên ===== */}
      {tab === 'students' && (
        <div className="space-y-4">
          <div className="flex justify-end">
            <button onClick={() => setEnrollOpen(true)} className={btnPrimary}><UserPlus className="w-4 h-4" /> Xếp học viên</button>
          </div>
          {roster.length === 0 ? (
            <div className="bg-white rounded-xl border border-slate-200"><Empty text="Lớp chưa có học viên." /></div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-50 text-left text-xs font-bold text-slate-500 uppercase tracking-wider border-b border-slate-200">
                    <th className="px-5 py-3">Học viên</th>
                    <th className="px-5 py-3">Liên hệ</th>
                    <th className="px-5 py-3">Vào lớp</th>
                    <th className="px-5 py-3 text-right">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {roster.map((s) => (
                    <tr key={s.studentId}>
                      <td className="px-5 py-3 font-semibold text-slate-800">{s.fullName}</td>
                      <td className="px-5 py-3 text-xs text-slate-500">{s.email}{s.phone ? ` • ${s.phone}` : ''}</td>
                      <td className="px-5 py-3 text-xs text-slate-500">{fmtDate(s.joinedAt)}</td>
                      <td className="px-5 py-3 text-right">
                        <button onClick={() => { setActionError(''); setRemoveTarget(s); }} title="Rút khỏi lớp" className="p-2 rounded-lg text-rose-600 hover:bg-rose-50 border border-rose-200">
                          <UserMinus className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ===== Bài tập (chỉ đọc) ===== */}
      {tab === 'assignments' && (
        <div className="space-y-3">
          <p className="text-xs text-slate-500">Giảng viên giao và chấm bài trên app Android. Admin chỉ theo dõi.</p>
          {assignments.length === 0 ? (
            <div className="bg-white rounded-xl border border-slate-200"><Empty text="Lớp chưa có bài tập." /></div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-50 text-left text-xs font-bold text-slate-500 uppercase tracking-wider border-b border-slate-200">
                    <th className="px-5 py-3">Bài tập</th>
                    <th className="px-5 py-3">Hạn nộp</th>
                    <th className="px-5 py-3 text-center">Đã nộp</th>
                    <th className="px-5 py-3 text-center">Đã chấm</th>
                    <th className="px-5 py-3 text-right">Bài nộp</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {assignments.map((a) => (
                    <tr key={a.id}>
                      <td className="px-5 py-3 font-semibold text-slate-800">{a.title}</td>
                      <td className={`px-5 py-3 text-xs ${a.minutesLeft != null && a.minutesLeft < 0 ? 'text-rose-600' : 'text-slate-600'}`}>{fmtDateTime(a.deadline)}</td>
                      <td className="px-5 py-3 text-center">{a.submittedCount}/{a.totalStudents}</td>
                      <td className="px-5 py-3 text-center">{a.gradedCount}</td>
                      <td className="px-5 py-3 text-right">
                        <button onClick={() => setSubmissionsFor(a.id)} className="p-2 rounded-lg text-brand-600 hover:bg-brand-50 border border-brand-100" title="Xem bài nộp">
                          <Eye className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {tab === 'materials' && (
        <div className="space-y-4">
          <div className="flex justify-end">
            <button onClick={() => setMaterialFormOpen(true)} className={btnPrimary}><Plus className="w-4 h-4" /> Thêm tài liệu</button>
          </div>
          {materials.length === 0 ? (
            <div className="bg-white rounded-xl border border-slate-200"><Empty text="Lớp chưa có tài liệu." /></div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-50 text-left text-xs font-bold text-slate-500 uppercase tracking-wider border-b border-slate-200">
                    <th className="px-5 py-3">Tài liệu</th>
                    <th className="px-5 py-3">Người đăng</th>
                    <th className="px-5 py-3">Ngày đăng</th>
                    <th className="px-5 py-3 text-right">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {materials.map((m) => (
                    <tr key={m.id}>
                      <td className="px-5 py-3">
                        <p className="font-semibold text-slate-800">{m.title}</p>
                        {m.description && <p className="text-xs text-slate-500">{m.description}</p>}
                      </td>
                      <td className="px-5 py-3 text-xs text-slate-600">{m.uploadedByName || '—'}</td>
                      <td className="px-5 py-3 text-xs text-slate-500">{fmtDateTime(m.createdAt)}</td>
                      <td className="px-5 py-3">
                        <div className="flex justify-end gap-2">
                          <a href={m.fileUrl} target="_blank" rel="noreferrer" title="Mở file" className="p-2 rounded-lg text-brand-600 hover:bg-brand-50 border border-brand-100"><ExternalLink className="w-4 h-4" /></a>
                          <button onClick={() => { setActionError(''); setDeleteMaterial(m); }} title="Xóa" className="p-2 rounded-lg text-rose-600 hover:bg-rose-50 border border-rose-200"><Trash2 className="w-4 h-4" /></button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ===== Modals ===== */}
      <CourseFormModal isOpen={courseFormOpen} onClose={() => setCourseFormOpen(false)} course={course} teachers={teacherList} onSaved={refreshAll} />
      <LessonFormModal isOpen={lessonForm.open} onClose={() => setLessonForm({ open: false, lesson: null })} courseId={courseId} lesson={lessonForm.lesson} onSaved={refreshAll} />
      <GenerateLessonsModal isOpen={genOpen} onClose={() => setGenOpen(false)} courseId={courseId} defaultSessions={course.totalSessions} onSaved={refreshAll} />
      <EnrollStudentsModal isOpen={enrollOpen} onClose={() => setEnrollOpen(false)} courseId={courseId} enrolledIds={roster.map((s) => s.studentId)} onSaved={refreshAll} />
      <SubmissionsModal isOpen={!!submissionsFor} assignmentId={submissionsFor} onClose={() => setSubmissionsFor(null)} />
      <MaterialFormModal isOpen={materialFormOpen} onClose={() => setMaterialFormOpen(false)} courseId={courseId} onSaved={refreshAll} />
      <ConfirmModal
        isOpen={!!cancelTarget}
        title="Hủy buổi học"
        danger
        confirmText="Hủy buổi"
        loading={actionLoading}
        error={actionError}
        onClose={() => setCancelTarget(null)}
        onConfirm={doCancelLesson}
        message={cancelTarget && <>Hủy buổi học ngày <strong>{fmtDateTime(cancelTarget.startTime)}</strong>? Học viên trong lớp sẽ nhận thông báo.</>}
      />
      <ConfirmModal
        isOpen={!!removeTarget}
        title="Rút học viên khỏi lớp"
        danger
        confirmText="Rút khỏi lớp"
        loading={actionLoading}
        error={actionError}
        onClose={() => setRemoveTarget(null)}
        onConfirm={doRemoveStudent}
        message={removeTarget && <>Rút <strong>{removeTarget.fullName}</strong> khỏi lớp <strong>{course.courseName}</strong>? Lịch sử bài nộp và điểm danh vẫn được giữ.</>}
      />
      <ConfirmModal
        isOpen={!!deleteMaterial} title="Xóa tài liệu" danger confirmText="Xóa"
        loading={actionLoading} error={actionError}
        onClose={() => setDeleteMaterial(null)} onConfirm={doDeleteMaterial}
        message={deleteMaterial && <>Xóa tài liệu <strong>{deleteMaterial.title}</strong>? File sẽ bị xóa khỏi kho lưu trữ.</>}
      />
    </div>
  );
}
