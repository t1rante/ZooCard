import { Routes, Route, Navigate, useParams } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import LoginScreen from './screens/LoginScreen';
import HomeScreen from './screens/HomeScreen';
import CollectionsScreen from './screens/CollectionsScreen';
import CollectionScreen from './screens/CollectionScreen';
import CardScreen from './screens/CardScreen';
import ProfileScreen from './screens/ProfileScreen';

// Remonta a tela quando o parâmetro de rota muda, para que cada tela possa
// resetar seu próprio estado de loading/erro sem setState síncrono no efeito.
function KeyedCollectionScreen() {
  const { colecaoId } = useParams();
  return <CollectionScreen key={colecaoId} />;
}

function KeyedCardScreen() {
  const { colecaoId, cartaId } = useParams();
  return <CardScreen key={`${colecaoId}/${cartaId}`} />;
}

function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4">
        <div className="panel-neo p-8 text-center">
          <p className="font-black uppercase">Carregando…</p>
        </div>
      </div>
    );
  }

  if (!user) return <Navigate to="/login" replace />;

  return children;
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginScreen />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <HomeScreen />
          </ProtectedRoute>
        }
      />
      <Route
        path="/colecoes"
        element={
          <ProtectedRoute>
            <CollectionsScreen />
          </ProtectedRoute>
        }
      />
      <Route
        path="/colecoes/:colecaoId"
        element={
          <ProtectedRoute>
            <KeyedCollectionScreen />
          </ProtectedRoute>
        }
      />
      <Route
        path="/colecoes/:colecaoId/cartas/:cartaId"
        element={
          <ProtectedRoute>
            <KeyedCardScreen />
          </ProtectedRoute>
        }
      />
      <Route
        path="/perfil"
        element={
          <ProtectedRoute>
            <ProfileScreen />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
