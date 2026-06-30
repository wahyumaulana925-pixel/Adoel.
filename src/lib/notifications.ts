import { LocalNotifications } from '@capacitor/local-notifications'
import { shiftAbsKeJamStr } from '../store/useDoffStore'

export async function ensurePermission(): Promise<boolean> {
  const check = await LocalNotifications.checkPermissions()
  if (check.display === 'granted') return true
  const req = await LocalNotifications.requestPermissions()
  return req.display === 'granted'
}

export async function checkPermission(): Promise<boolean> {
  const r = await LocalNotifications.checkPermissions()
  return r.display === 'granted'
}

// Schedules a notification 5 minutes before estAbsMin.
// Uses parseInt(mcNo) as the notification ID so each machine has at most
// one pending notification — scheduling again overwrites the previous one.
export async function scheduleNotif(mcNo: string, estAbsMin: number): Promise<void> {
  const fireAt = estAbsMin * 60000 - 5 * 60 * 1000
  if (fireAt <= Date.now()) return
  await LocalNotifications.schedule({
    notifications: [
      {
        id: parseInt(mcNo, 10),
        title: `Mc ${mcNo} siap doffing`,
        body: `Estimasi pukul ${shiftAbsKeJamStr(estAbsMin)} — 5 menit lagi`,
        schedule: { at: new Date(fireAt), allowWhileIdle: true },
      },
    ],
  })
}

export async function cancelNotif(mcNo: string): Promise<void> {
  await LocalNotifications.cancel({ notifications: [{ id: parseInt(mcNo, 10) }] })
}

// Cancel notifications for multiple machines at once (e.g., before reset)
export async function cancelAllNotifs(mcNos: string[]): Promise<void> {
  if (mcNos.length === 0) return
  await LocalNotifications.cancel({
    notifications: mcNos.map((n) => ({ id: parseInt(n, 10) })),
  })
}
