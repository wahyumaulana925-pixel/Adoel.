import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { buildDefaultDb } from '../data/defaultDb'
import type { MesinDb, MesinData, Estimasi, AktualEntry } from '../types'

function pad2(n: number) {
  return n.toString().padStart(2, '0')
}

function parseJam(str: string): { h: number; m: number } | null {
  str = str.trim().replace(',', '.')
  let m = str.match(/^(\d{1,2})[.:](\d{2})$/)
  if (m) {
    const h = parseInt(m[1], 10)
    const mi = parseInt(m[2], 10)
    if (h >= 0 && h <= 23 && mi >= 0 && mi <= 59) return { h, m: mi }
    return null
  }
  m = str.match(/^(\d{2})(\d{2})$/)
  if (m) {
    const h = parseInt(m[1], 10)
    const mi = parseInt(m[2], 10)
    if (h >= 0 && h <= 23 && mi >= 0 && mi <= 59) return { h, m: mi }
    return null
  }
  return null
}

function jamToMinutes(jam: { h: number; m: number }) {
  return jam.h * 60 + jam.m
}

function parseDurasiJamMenit(str: string): number | null {
  if (!str) return null
  str = str.trim().replace(',', '.')
  let m = str.match(/^(\d{1,2})\.(\d{2})$/)
  if (m) {
    const h = parseInt(m[1], 10)
    const mi = parseInt(m[2], 10)
    if (mi >= 0 && mi <= 59) return h * 60 + mi
    return null
  }
  m = str.match(/^(\d{1,4})m(?:enit)?$/i)
  if (m) return parseInt(m[1], 10)
  m = str.match(/^(\d{1,4})$/)
  if (m) return parseInt(m[1], 10)
  return null
}

export function jamSekarangAbs() {
  return Math.floor(Date.now() / 60000)
}

export function jamSekarangLabel() {
  const now = new Date()
  return pad2(now.getHours()) + '.' + pad2(now.getMinutes())
}

export function jamKeShiftAbs(jamMin: number) {
  const now = new Date()
  const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const epochMinToday = Math.floor(startOfDay / 60000) + jamMin
  const currentEpochMin = jamSekarangAbs()
  const diff = epochMinToday - currentEpochMin
  if (diff < -720) return epochMinToday + 1440
  if (diff > 720) return epochMinToday - 1440
  return epochMinToday
}

export function shiftAbsKeJamStr(absMin: number) {
  const d = new Date(absMin * 60000)
  return pad2(d.getHours()) + '.' + pad2(d.getMinutes())
}

export function standarisasiKeterangan(raw: string): string {
  const t = raw.trim().toLowerCase()
  if (t === 'hb') return 'HB'
  if (t === 'lp' || t === 'p.lp' || t === 'p. lp' || t === 'p lp') return 'P.LP'
  if (t === 'sn' || t === 'p.sn' || t === 'p. sn' || t === 'p sn' || t === 'snarling') return 'P.SN'
  if (t === 'oh' || t === 'p.oh' || t === 'p. oh' || t === 'p oh' || t === 'overhaul') return 'P.OH'
  if (t === 'el' || t === 'p.el' || t === 'p. el' || t === 'p el' || t === 'elektrik') return 'P.EL'
  if (t === 'sel' || t === 'selvedge' || t === 'p.sel' || t === 'p. sel' || t === 'p sel') return 'P.Sel'
  return raw.trim()
}

export interface ProsesResult {
  type: 'ok' | 'err'
  msg: string
  mcNo?: string
  estAbs?: number      // returned by prosesBarisKondisiMesin for notification scheduling
  prevEst?: Estimasi   // returned by prosesBarisUmum so caller can reschedule on undo
  undoFn?: () => void
}

interface DoffStore {
  db: MesinDb
  estimasi: Record<string, Estimasi>
  aktual: AktualEntry[]
  _nextId: number

  setMesin: (mcNo: string, data: MesinData) => void
  resetMesin: (mcNo: string) => void
  resetDb: () => void

  prosesBarisKondisiMesin: (ln: string, jamKelilingAbs: number) => ProsesResult
  prosesBarisUmum: (ln: string) => ProsesResult

  hapusEstimasi: (mcNo: string) => void
  restoreEstimasi: (est: Estimasi) => void
  updateEstimasi: (mcNo: string, data: Partial<Estimasi>) => void

  hapusAktualById: (id: number) => void
  updateAktual: (id: number, data: Partial<AktualEntry>) => void
  restoreAktual: (entry: AktualEntry) => void

  finishShift: () => void
}

export const useDoffStore = create<DoffStore>()(
  persist(
    (set, get) => ({
      db: buildDefaultDb(),
      estimasi: {},
      aktual: [],
      _nextId: 1,

      setMesin: (mcNo, data) =>
        set((state) => ({ db: { ...state.db, [mcNo]: data } })),

      resetMesin: (mcNo) =>
        set((state) => {
          const defaultDb = buildDefaultDb()
          const fallback = defaultDb[mcNo] ?? { tipe: 'TAPPET' as const, corak: '-' }
          return { db: { ...state.db, [mcNo]: fallback } }
        }),

      resetDb: () => set({ db: buildDefaultDb(), estimasi: {}, aktual: [], _nextId: 1 }),

      prosesBarisKondisiMesin: (ln, jamKelilingAbs) => {
        const parts = ln.trim().split(/\s+/)
        if (parts.length < 2) return { type: 'err', msg: 'Kurang data' }
        const mcNo = parts[0]
        if (!/^\d{1,3}$/.test(mcNo)) return { type: 'err', msg: 'Nomor mesin tidak valid' }
        const mesin = get().db[mcNo]
        if (!mesin) return { type: 'err', msg: `Mc ${mcNo} tidak ditemukan` }
        if (!mesin.corak || mesin.corak.trim() === '-')
          return { type: 'err', msg: `Mc ${mcNo} dilewati (-)` }

        let estAbs: number | null = null

        if (mesin.tipe === 'TAPPET' || mesin.tipe === 'CAM') {
          const sisaMin = parseDurasiJamMenit(parts[1])
          if (sisaMin === null) return { type: 'err', msg: 'Durasi tidak valid' }
          estAbs = jamKelilingAbs + sisaMin
        } else if (mesin.tipe === 'D405') {
          const yardStr = parts[1].replace(/y$/i, '')
          const yardBerjalan = parseFloat(yardStr.replace(',', '.'))
          if (isNaN(yardBerjalan)) return { type: 'err', msg: 'Yard tidak valid' }
          const existing = get().estimasi[mcNo]
          const target = existing?.yardOverride ?? mesin.targetYard
          if (!target || !mesin.speed)
            return { type: 'err', msg: 'Data target/speed kosong' }
          const sisaMin = Math.round((target - yardBerjalan) / mesin.speed)
          estAbs = jamKelilingAbs + sisaMin
        } else if (mesin.tipe === 'D408') {
          let jamCounterStr = parts[1]
          if (jamCounterStr.toLowerCase() === 'c' && parts.length >= 3) jamCounterStr = parts[2]
          const jamCounter = parseJam(jamCounterStr)
          if (!jamCounter) return { type: 'err', msg: 'Jam counter tidak valid' }
          if (mesin.koreksi === undefined || mesin.koreksi === null)
            return { type: 'err', msg: 'Menit koreksi kosong' }
          estAbs = jamKeShiftAbs(jamToMinutes(jamCounter)) + mesin.koreksi
        } else {
          return { type: 'err', msg: 'Tipe mesin tidak dikenal' }
        }

        const existing = get().estimasi[mcNo]
        const newEstimasi: Estimasi = {
          mcNo,
          estAbsMin: Math.round(estAbs),
          startAbsMin: existing ? existing.startAbsMin : jamSekarangAbs(),
          corakOverride: existing?.corakOverride,
          yardOverride: existing?.yardOverride,
        }
        set((state) => ({ estimasi: { ...state.estimasi, [mcNo]: newEstimasi } }))

        return {
          type: 'ok',
          msg: `Mc ${mcNo} → ${shiftAbsKeJamStr(estAbs)}`,
          mcNo,
          estAbs: Math.round(estAbs),
        }
      },

      prosesBarisUmum: (ln) => {
        const parts = ln.trim().split(/\s+/)
        if (parts.length < 1) return { type: 'err', msg: 'Kosong' }
        const mcNo = parts[0]
        if (!/^\d{1,3}$/.test(mcNo)) return { type: 'err', msg: 'Nomor mesin tidak valid' }
        const mesin = get().db[mcNo]
        if (!mesin) return { type: 'err', msg: `Mc ${mcNo} tidak ditemukan` }

        if (mesin.tipe === 'D408' && parts[1]?.toLowerCase() === 'c') {
          return get().prosesBarisKondisiMesin(ln, jamSekarangAbs())
        }

        const jam = jamSekarangLabel()
        let customYard: number | undefined
        const ketTokens: string[] = []

        for (let i = 1; i < parts.length; i++) {
          const token = parts[i]
          // y suffix is optional — matches PWA original: /^(\+?)([\d.,]+)y?$/i
          const ydMatch = token.match(/^(\+?)([\d.,]+)y?$/i)
          if (ydMatch) {
            const y = parseFloat(ydMatch[2].replace(',', '.'))
            if (!isNaN(y)) { customYard = y; continue }
          }
          ketTokens.push(token)
        }

        const extra = standarisasiKeterangan(ketTokens.join(' ').trim())
        const ket = extra ? `${jam}(${extra})` : jam

        const prevEst = get().estimasi[mcNo]
        const effectiveCorak = prevEst?.corakOverride ?? mesin.corak
        let entryId = 0

        set((state) => {
          entryId = state._nextId
          const nextEst = { ...state.estimasi }
          delete nextEst[mcNo]
          const entry: AktualEntry = {
            id: entryId,
            mcNo,
            jam,
            ket,
            corakOverride: effectiveCorak !== mesin.corak ? effectiveCorak : undefined,
            customYard,
          }
          return {
            _nextId: entryId + 1,
            estimasi: nextEst,
            aktual: [entry, ...state.aktual],
          }
        })

        return {
          type: 'ok',
          msg: `Mc ${mcNo} ✓`,
          mcNo,
          prevEst: prevEst ?? undefined,
          undoFn: () => {
            get().hapusAktualById(entryId)
            if (prevEst) get().restoreEstimasi(prevEst)
          },
        }
      },

      hapusEstimasi: (mcNo) =>
        set((state) => {
          const next = { ...state.estimasi }
          delete next[mcNo]
          return { estimasi: next }
        }),

      restoreEstimasi: (est) =>
        set((state) => ({ estimasi: { ...state.estimasi, [est.mcNo]: est } })),

      updateEstimasi: (mcNo, data) =>
        set((state) => {
          const existing = state.estimasi[mcNo]
          if (!existing) return state
          return { estimasi: { ...state.estimasi, [mcNo]: { ...existing, ...data } } }
        }),

      hapusAktualById: (id) =>
        set((state) => ({ aktual: state.aktual.filter((a) => a.id !== id) })),

      updateAktual: (id, data) =>
        set((state) => ({
          aktual: state.aktual.map((a) => (a.id === id ? { ...a, ...data } : a)),
        })),

      restoreAktual: (entry) =>
        set((state) => {
          const arr = [...state.aktual]
          const pos = arr.findIndex((a) => a.id < entry.id)
          if (pos === -1) arr.push(entry)
          else arr.splice(pos, 0, entry)
          return { aktual: arr }
        }),

      finishShift: () => set({ aktual: [] }),
    }),
    { name: 'adoel-v5-storage-v2' }
  )
)
