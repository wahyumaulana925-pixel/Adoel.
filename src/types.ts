export type MesinTipe = 'TAPPET' | 'CAM' | 'D405' | 'D408'

export interface MesinData {
  tipe: MesinTipe
  corak: string
  targetYard?: number
  speed?: number
  koreksi?: number
}

export type MesinDb = Record<string, MesinData>

export interface Estimasi {
  mcNo: string
  estAbsMin: number
  startAbsMin: number
  corakOverride?: string
  yardOverride?: number
}

export interface AktualEntry {
  id: number
  mcNo: string
  jam: string
  ket: string
  corakOverride?: string
  customYard?: number
}
