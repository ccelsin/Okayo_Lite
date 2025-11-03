import { BrowserRouter, Route, Routes } from "react-router-dom";
import ProtectedRoute from "../components/ProtectedRoute";
import LoginPage from "../pages/shared/LoginPage";
import RegisterPage from "../pages/shared/RegisterPage";
import DashboardPage from "../pages/shared/DashboardPage";
import ProductsPage from "../pages/admin/ProductsPage";
import PurchasesPage from "../pages/admin/PurchasesPage";
import MyPurchasesPage from "../pages/customer/MyPurchasesPage";
import PaymentDetailsPage from "../pages/admin/PaymentDetailsPage";
import TvaPage from "../pages/admin/TvaPage";
import ProfilePage from "../pages/shared/ProfilePage";

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
            <ProtectedRoute>
              <TvaPage />
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
      </Routes>
    </BrowserRouter>
  );
}
