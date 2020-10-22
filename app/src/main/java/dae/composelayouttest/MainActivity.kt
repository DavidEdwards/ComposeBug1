package dae.composelayouttest

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnForIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.onActive
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dae.composelayouttest.ui.ComposeLayoutTestTheme
import dev.chrisbanes.accompanist.coil.CoilImage
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeLayoutTestTheme {
                Scaffold(
                        topBar = {
                            TopAppBar(
                                    title = {
                                        Text(text = getString(R.string.app_name))
                                    },
                                    backgroundColor = MaterialTheme.colors.background,
                                    contentColor = MaterialTheme.colors.onBackground,
                                    elevation = 6.dp
                            )
                        },
                        bodyContent = {
                            // A surface container using the 'background' color from the theme
                            Surface(color = MaterialTheme.colors.background) {
                                Column {
                                    Container()
                                }
                            }
                        }
                )
            }
        }
    }
}


@Composable
fun Container() {
    onActive(callback = {
        Log.v("TESTTEST", "View · Container")
    })

    val list = MutableList(2) { it + 1 }

    LazyColumnForIndexed(items = list) { index, item ->
        Card(item = item)
    }
}

@Composable
fun Card(item: Int) {
    onActive(callback = {
        Log.v("TESTTEST", "View · Card · $item")
    })

    Surface(
            Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
            elevation = 4.dp
    ) {
        Box(
                Modifier
                        .fillMaxSize()
                        .clickable(onClick = {
                            Log.v("TESTTEST", "View · Card · Clicked")
                        })
                        .padding(8.dp)
        ) {
            // Two examples of the issue
            if (item % 2 == 1) {
                // The first example shows a blue background for Details. The Details sibling,
                // I would expect to expand to the height of the parent (which has grown due to
                // the image loading). In this case, Button 1 should be at the bottom right of
                // the card.
                Row(
                        modifier = Modifier.fillMaxHeight()
                ) {
                    CoverImage()
                    Spacer(modifier = Modifier.width(8.dp))
                    Details()
                }
            } else {
                // The second example includes a custom layout that should indicate that logs out
                // when the layout is laying out.
                Row(
                        modifier = Modifier.fillMaxHeight()
                ) {
                    // This composable will cause the Row to grow to the size of the image
                    CoilImage(
                            data = "https://picsum.photos/id/15/400/700",
                            loading = {
                                Box(
                                        Modifier
                                                .width(100.dp)
                                                .height(300.dp)
                                                .background(Color.Red)
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // This composable will start out fitting to the height of the
                    // text (since there is nothing yet loaded to compare with). Once
                    // the image is rendered, the box will not continue to fill the space.
                    LoggingBox(
                            modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Blue)
                    ) {
                        Text(
                                text = "This is an example"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoverImage() {
    val imageUrl = "https://picsum.photos/id/1/400/700"

    onActive(callback = {
        Log.v("TESTTEST", "View · CoverImage · $imageUrl")
    })

    Box(
            Modifier.width(120.dp),
            alignment = Alignment.Center
    ) {
        CoilImage(
                data = imageUrl,
                fadeIn = true
        )
    }
}

@Composable
fun Details() {
    ConstraintLayout(
            modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(Color.Blue)
    ) {
        onActive(callback = {
            Log.v("TESTTEST", "View · Details")
        })

        val (text1, text2, button1) = createRefs()

        Text(
                modifier = Modifier.constrainAs(text1) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                }.padding(bottom = 8.dp),
                style = TextStyle(fontWeight = FontWeight.Bold),
                text = "Text 1"
        )
        Text(
                modifier = Modifier.constrainAs(text2) {
                    top.linkTo(text1.bottom)
                    start.linkTo(parent.start)
                },
                text = "Text 2"
        )

        Button(
                modifier = Modifier.constrainAs(button1) {
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                },
                onClick = {
                    Log.v("TESTTEST", "Clicked on button")
                }
        ) {
            Text(text = "Button 1")
        }
    }
}


// Ignore all this. I copied the Box implementation to add logging.

private data class BoxChildData(
        var alignment: Alignment,
        var stretch: Boolean = false
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@BoxChildData
}

private val Measurable.boxChildData: BoxChildData? get() = parentData as? BoxChildData
private val Measurable.stretch: Boolean get() = boxChildData?.stretch ?: false

@Composable
fun LoggingBox(
        modifier: Modifier = Modifier,
        alignment: Alignment = Alignment.TopStart,
        children: @Composable BoxScope.() -> Unit
) {
    val boxChildren: @Composable () -> Unit = { BoxScope.children() }

    Layout(boxChildren, modifier = modifier) { measurables, constraints ->
        Log.v("TESTTEST", "LAYOUT $constraints")
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        // First measure aligned children to get the size of the layout.
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        (0 until measurables.size).filter { i -> !measurables[i].stretch }.forEach { i ->
            placeables[i] = measurables[i].measure(childConstraints)
        }
        val (boxWidth, boxHeight) = with(placeables.filterNotNull()) {
            Pair(
                    max(maxByOrNull { it.width }?.width ?: 0, constraints.minWidth),
                    max(maxByOrNull { it.height }?.height ?: 0, constraints.minHeight)
            )
        }

        // Now measure stretch children.
        (0 until measurables.size).filter { i -> measurables[i].stretch }.forEach { i ->
            // infinity check is needed for intrinsic measurements
            val minWidth = if (boxWidth != androidx.compose.ui.unit.Constraints.Infinity) boxWidth else 0
            val minHeight = if (boxHeight != androidx.compose.ui.unit.Constraints.Infinity) boxHeight else 0
            placeables[i] = measurables[i].measure(
                    androidx.compose.ui.unit.Constraints(minWidth, boxWidth, minHeight, boxHeight)
            )
        }

        // Position the children.
        layout(boxWidth, boxHeight) {
            (0 until measurables.size).forEach { i ->
                val measurable = measurables[i]
                val childAlignment = measurable.boxChildData?.alignment ?: alignment
                val placeable = placeables[i]!!

                val position = childAlignment.align(
                        IntSize(
                                boxWidth - placeable.width,
                                boxHeight - placeable.height
                        ),
                        layoutDirection
                )
                placeable.place(position.x, position.y)
            }
        }
    }
}