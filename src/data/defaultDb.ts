import type { MesinDb } from '../types'

export function buildDefaultDb(): MesinDb {
  const db: MesinDb = {}
  for (let i = 1; i <= 174; i++) db[i] = { tipe: 'TAPPET', corak: '-' }

  ;[29, 30, 31, 32, 33, 34, 35, 39, 42, 43, 45, 47, 49, 51, 53].forEach(
    (i) => (db[i] = { tipe: 'TAPPET', corak: '34758', targetYard: 303.0 })
  )
  db[36] = { tipe: 'TAPPET', corak: '15976', targetYard: 303.0 }
  ;[37, 38, 40, 41].forEach(
    (i) => (db[i] = { tipe: 'TAPPET', corak: '92976', targetYard: 303.0 })
  )
  db[55] = { tipe: 'TAPPET', corak: '35758', targetYard: 303.0 }

  ;[57, 58, 59, 60, 63, 64, 71, 72].forEach((i) => (db[i] = { tipe: 'D405', corak: '-' }))
  ;[61, 62, 68].forEach(
    (i) => (db[i] = { tipe: 'D405', corak: '60357', targetYard: 303.0, speed: 0.158 })
  )
  ;[65, 70].forEach(
    (i) => (db[i] = { tipe: 'D405', corak: '60357', targetYard: 303.0, speed: 0.156 })
  )
  ;[66, 69].forEach(
    (i) => (db[i] = { tipe: 'D405', corak: '60357', targetYard: 303.0, speed: 0.1625 })
  )
  db[67] = { tipe: 'D405', corak: '60357', targetYard: 303.0, speed: 0.145 }
  db[85] = { tipe: 'D405', corak: '60357', targetYard: 303.0, speed: 0.154 }
  db[86] = { tipe: 'D405', corak: '60357', targetYard: 303.0, speed: 0.146 }
  db[87] = { tipe: 'D405', corak: '60357', targetYard: 303.0, speed: 0.158 }
  db[88] = { tipe: 'D405', corak: '60357', targetYard: 303.0, speed: 0.152 }

  ;[73, 74, 75, 77, 78, 89, 90, 91, 92, 93, 94, 117, 119].forEach(
    (i) => (db[i] = { tipe: 'CAM', corak: '-' })
  )
  ;[76, 95, 96, 97, 99, 100, 102].forEach(
    (i) => (db[i] = { tipe: 'CAM', corak: '21242', targetYard: 165.0 })
  )
  ;[98, 101, 103, 104].forEach(
    (i) => (db[i] = { tipe: 'CAM', corak: '66335', targetYard: 165.0 })
  )

  db[79] = { tipe: 'D408', corak: '60357', targetYard: 303.0, koreksi: 18.0 }
  db[80] = { tipe: 'D408', corak: '60357', targetYard: 303.0, koreksi: 16.0 }
  db[81] = { tipe: 'D408', corak: '60357', targetYard: 303.0, koreksi: 22.0 }
  db[82] = { tipe: 'D408', corak: '60357', targetYard: 303.0, koreksi: 17.0 }
  db[83] = { tipe: 'D408', corak: '60357', targetYard: 303.0, koreksi: 16.0 }
  db[84] = { tipe: 'D408', corak: '60357', targetYard: 303.0, koreksi: 23.0 }

  return db
}
