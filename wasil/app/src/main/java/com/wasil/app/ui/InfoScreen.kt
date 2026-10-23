package com.wasil.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wasil.app.R
import com.wasil.app.ui.components.InfoBanner
import com.wasil.app.ui.components.TopBar
import com.wasil.app.ui.theme.Coral
import com.wasil.app.ui.theme.Gold
import com.wasil.app.ui.theme.Mint
import com.wasil.app.ui.theme.Sky
import com.wasil.app.ui.theme.SurfaceOne
import com.wasil.app.ui.theme.SurfaceTwo
import com.wasil.app.ui.theme.TextMuted
import com.wasil.app.ui.theme.TextPrimary

private data class InfoItem(val icon: Int, val accent: Color, val title: String, val body: String)

@Composable
fun InfoScreen(onBack: () -> Unit) {
    val items = listOf(
        InfoItem(
            R.drawable.ic_shield, Mint,
            "١) من أين تأتي الأرقام الحقيقية؟",
            "أرقام الهواتف الأمريكية تمنحها شركات اتصالات ومزوّدو خدمة مرخّصون فقط (مثل Twilio وTelnyx وGoogle Voice). لا يوجد أي تطبيق يستطيع «توليد» رقم حقيقي بنفسه — أي تطبيق يدّعي ذلك يمنحك إما رقم مشترك مع الغير أو خدمة وسيطة محفوفة بالحظر."
        ),
        InfoItem(
            R.drawable.ic_globe, Sky,
            "٢) ماذا يفعل واصل إذن؟",
            "واصل صندوق وارد عربي أنيق لرقم تملكه أنت: تشتري رقمك الأمريكي بحسابك الخاص لدى مزوّد مرخّص، يضع المزوّد الرسائل على خادمك الصغير، ويعرضها التطبيق هنا مع كشف كود التفعيل ونسخه بضغطة. كل المنظومة باسمك وعلى بنيتك."
        ),
        InfoItem(
            R.drawable.ic_warning, Coral,
            "٣) تنبيه واتساب وفايبر — اقرأ بعناية",
            "أرقام خدمات الإنترنت (VoIP) ومنها أغلب أرقام Twilio قد يرفضها واتساب أو فايبر عند التفعيل، أو يقبلها ثم يحظر الحساب لاحقاً، لأن شروط استخدامهما تفضّل أرقام الهواتف المحمولة العادية. النجاح غير مضمون، ولا أحد يستطيع ضده. جرّب رقماً واحداً أولاً قبل شراء رصيد كبير."
        ),
        InfoItem(
            R.drawable.ic_phone, Gold,
            "٤) أي المزوّدين أوفر حظاً للتفعيل؟",
            "• Google Voice: رقم أمريكي حقيقي مجاني تقريباً، والأكثر قبولاً لدى واتساب، لكنه يتطلب وجوداً أمريكياً (IP أمريكي) ورقماً أمريكياً للتحقق أول مرة، ولا يوفر API لذا يُستخدم عبر تطبيقه الرسمي وليس عبر واصل.\n\n• Twilio: الأشهر للمطوّرين، API ممتاز ويكلف نحو 1.15 دولار شهرياً للرقم مع سنتات لكل رسالة — وهو الخيار الذي يدعمه واصل برمجياً.\n\n• Telnyx وPlivo: بديلان مشابهان لـ Twilio بنفس الفكرة.\n\n• TextNow / TextFree: أرقام مجانية سهلة، لكن نتائجها مع التفعيل مختلطة وكثيرة الحظر."
        ),
        InfoItem(
            R.drawable.ic_check, Mint,
            "٥) لماذا هذه الطريقة مسموحة؟",
            "لأنك تستخدم رقماً اشتريته بحسابك الرسمي، وخادماً تملكه، وتطبيقاً مفتوح المصدر يخزّن بياناتك على جهازك. لا اختراق، لا أرقام مسروقة، لا مشاركة مع الغير. يبقى التزامك بشروط واتساب وفايبر عند استخدام الرقم في تفعيل حساباتهما مسؤوليتك."
        ),
    )

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "معلومات مهمة", onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 30.dp),
        ) {
            InfoBanner(
                text = "خلاصة صادقة: واصل يضمن لك استقبال رسائل رقمك الخاص بشكل منظّم وآمن، لكن لا أحد يضمن قبول واتساب أو فايبر لرقم إنترنت.",
                accent = Gold,
                icon = painterResource(R.drawable.ic_info),
            )
            Spacer(Modifier.height(16.dp))

            items.forEach { item ->
                InfoCard(item)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(item: InfoItem) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceOne)
            .padding(18.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceTwo),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painterResource(item.icon),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(item.accent),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                item.body,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
    }
}
