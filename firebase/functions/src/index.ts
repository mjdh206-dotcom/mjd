import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions";

initializeApp();
const db = getFirestore();

export const notifyStoreOfNewOrder = onDocumentCreated(
  { document: "orders/{orderId}", region: "europe-west1" },
  async (event) => {
    const order = event.data?.data();
    if (!order || order.status !== "جديد") return;
    const orderId = event.params.orderId;
    let validatedLines: Array<{ productId: string; name: string; qty: number; price: number }> = [];
    let total = 0;
    let valid = Array.isArray(order.lines) && order.lines.length > 0 && order.lines.length <= 40;

    if (valid) {
      for (const candidate of order.lines as unknown[]) {
        if (!candidate || typeof candidate !== "object") { valid = false; break; }
        const line = candidate as Record<string, unknown>;
        const productId = typeof line.productId === "string" ? line.productId : "";
        const qty = typeof line.qty === "number" ? line.qty : 0;
        if (!/^[A-Za-z0-9_-]{1,80}$/.test(productId) || !Number.isInteger(qty) || qty < 1 || qty > 99) { valid = false; break; }
        const productSnap = await db.collection("products").doc(productId).get();
        const product = productSnap.data();
        if (!productSnap.exists || !product || product.available !== true || typeof product.price !== "number" || !Number.isFinite(product.price) || product.price < 0) { valid = false; break; }
        const discount = typeof product.discount === "number" ? Math.min(90, Math.max(0, product.discount)) : 0;
        const unitPrice = Math.round(product.price * (100 - discount)) / 100;
        const name = typeof product.name === "string" ? product.name.slice(0, 100) : "منتج";
        validatedLines.push({ productId, name, qty, price: unitPrice });
        total += unitPrice * qty;
      }
    }

    const reviewRequired = !valid;
    await event.data?.ref.update(reviewRequired ? {
      status: "مراجعة يدوية",
      serverValidatedAt: Timestamp.now(),
    } : {
      lines: validatedLines,
      total: Math.round(total * 100) / 100,
      serverValidatedAt: Timestamp.now(),
    });

    try {
      await getMessaging().send({
        topic: "store_orders",
        notification: {
          title: reviewRequired ? "طلب يحتاج مراجعة — محامص الحرمين" : "طلب جديد — محامص الحرمين",
          body: `وصلك طلب ${reviewRequired ? "يتطلب التحقق" : "جديد"} رقم ${orderId.slice(-8).toUpperCase()}`,
        },
        data: { orderId, event: reviewRequired ? "review_order" : "new_order" },
        android: {
          priority: "high",
          notification: {
            channelId: "haramain_orders",
            sound: "default",
            visibility: "private",
          },
        },
      });
    } catch (error) {
      logger.error("Failed to send new-order FCM notification", { orderId, error });
      throw error;
    }
  },
);
