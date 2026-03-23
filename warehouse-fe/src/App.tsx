import AppRouter from './routes'
import ForceChangePasswordModal from './components/common/ForceChangePasswordModal'

const App = () => {
  return (
    <>
      <AppRouter />
      <ForceChangePasswordModal />
    </>
  )
}

export default App
