export function Placeholder({ title }: { title: string }) {
  return (
    <>
      <div className="title">{title}</div>
      <div className="card muted">Esta tela chega na próxima rodada.</div>
    </>
  )
}
