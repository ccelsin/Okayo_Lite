import { BrowserRouter, Route, Routes } from "react-router-dom";
import ProtectedRoute from "../components/ProtectedRoute";
import LoginPage from "../pages/LoginPage";
import RegisterPage from "../pages/RegisterPage";
import DashboardPage from "../pages/DashboardPage";
import ProductsPage from "../pages/ProductsPage";
import PurchasesPage from "../pages/PurchasesPage";
import InvoicesPage from "../pages/InvoicesPage";
import InvoiceCreateAssignPage from "../pages/InvoiceCreateAssignPage";
import MyPurchasesPage from "../pages/MyPurchasesPage";
import PaymentDetailsPage from "../pages/PaymentDetailsPage";
import TvaPage from "../pages/TvaPage";
import ProfilePage from "../pages/ProfilePage";
import ShoppingPage from "../pages/ShoppingPage";
import MyInvoices from "../pages/MyInvoices";
import InvoiceEdit from "../pages/InvoiceEdit";

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/products"
          element={
            <ProtectedRoute>
              <ProductsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/purchases"
          element={
            <ProtectedRoute>
              <PurchasesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/my-purchases"
          element={
            <ProtectedRoute>
              <MyPurchasesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/payment-details"
          element={
            <ProtectedRoute>
              <PaymentDetailsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/tva"
          element={
            <ProtectedRoute adminOnly>
              <TvaPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/invoices"
          element={
            <ProtectedRoute adminOnly>
              <InvoicesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/shop"
          element={
            <ProtectedRoute>
              <ShoppingPage />
            </ProtectedRoute>
          }
        />
        <Route 
          path="/invoices/new" 
          element={<ProtectedRoute>
                    <InvoiceCreateAssignPage />
                  </ProtectedRoute>} />

        <Route 
          path="/my-invoices" 
          element={<ProtectedRoute>
                    <MyInvoices />
                  </ProtectedRoute>} />

        <Route 
          path="/invoices/:id/edit" 
          element={<ProtectedRoute>
                    <InvoiceEdit />
                  </ProtectedRoute>} />
        
        

      </Routes>
    </BrowserRouter>
  );
}
