import Sidebar from './Sidebar';
import Header  from './Header';
import './Layout.css';

export default function AppLayout({ children, title, onCollectionSearch }) {
  return (
    <div className="layout">
      <Sidebar />
      <Header title={title} onCollectionSearch={onCollectionSearch} />
      <main className="layout__main">
        {children}
      </main>
    </div>
  );
}
