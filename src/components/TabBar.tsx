type TabName = 'utama' | 'riwayat' | 'mesin'

interface TabBarProps {
  activeTab: TabName
  onTabChange: (tab: TabName) => void
}

export function TabBar({ activeTab, onTabChange }: TabBarProps) {
  const tabs: TabName[] = ['utama', 'riwayat', 'mesin']

  return (
    <div style={{ display: 'flex', borderBottom: '1px solid #eee', maxWidth: 480, margin: '0 auto' }}>
      {tabs.map((t) => (
        <button
          key={t}
          onClick={() => onTabChange(t)}
          style={{
            flex: 1,
            padding: 12,
            background: activeTab === t ? '#0066ff' : '#fff',
            color: activeTab === t ? '#fff' : '#333',
            fontWeight: 600,
            textTransform: 'capitalize',
            border: 'none',
            cursor: 'pointer',
            transition: 'all .2s',
          }}
        >
          {t}
        </button>
      ))}
    </div>
  )
}
