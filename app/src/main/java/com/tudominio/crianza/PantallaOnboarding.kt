package com.tudominio.crianza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tudominio.crianza.ui.theme.*

private data class OnboardingSlide(
    val emoji: String,
    val titulo: String,
    val descripcion: String
)

private val slides = listOf(
    OnboardingSlide(
        "🌱",
        "Co-parenting sin caos",
        "Nesty ordena la crianza compartida entre dos hogares: calendario, gastos, tareas y compensaciones automáticas."
    ),
    OnboardingSlide(
        "👪",
        "Conectá a tu familia",
        "En Configuración tenés un código familiar. Compartilo con el otro adulto y todo se sincroniza en tiempo real."
    ),
    OnboardingSlide(
        "📑",
        "Evidencia legal lista",
        "Tus gastos, días de custodia y cambios quedan registrados con hash. Exportás PDF firmado para mediación o juzgado."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaOnboarding(onTerminar: () -> Unit) {
    var indice by rememberSaveable { mutableStateOf(0) }
    val slide = slides[indice]
    val ultimo = indice == slides.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGradientMain)
    ) {
        // Skip top right
        TextButton(
            onClick = onTerminar,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 32.dp, end = 16.dp)
        ) {
            Text("Saltar", color = NeutralVariant50, fontWeight = FontWeight.Medium)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0x33648646)),
                contentAlignment = Alignment.Center
            ) {
                Text(slide.emoji, fontSize = 72.sp)
            }

            Spacer(Modifier.height(40.dp))

            Text(
                slide.titulo,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Indigo20,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                slide.descripcion,
                fontSize = 16.sp,
                color = NeutralVariant30,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }

        // Bottom: dots + next button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${indice + 1} / ${slides.size}",
                fontSize = 13.sp,
                color = NeutralVariant50,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (indice > 0) {
                    OutlinedButton(
                        onClick = { indice-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Anterior", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Indigo40)
                    }
                }
                Button(
                    onClick = {
                        if (ultimo) onTerminar()
                        else indice++
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo40)
                ) {
                    Text(
                        if (ultimo) "Empezar" else "Siguiente",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
