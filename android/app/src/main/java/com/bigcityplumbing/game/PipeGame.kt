package com.bigcityplumbing.game

import android.content.Context
import com.bigcityplumbing.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Pipe Drop — marble-run pipe puzzle. Player rotates tiles to form a continuous
 * path from the entry funnel (top) to the exit funnel (bottom). When they hit
 * Drop, the marble rolls along the connected path and either reaches the exit
 * (win) or falls off mid-route (fail).
 */

// region Sides

enum class Side(val dx: Int, val dy: Int) {
    UP(0, -1), RIGHT(1, 0), DOWN(0, 1), LEFT(-1, 0);

    fun opposite(): Side = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }

    /** Rotate by N 90° clockwise steps. */
    fun rotated(steps: Int): Side {
        val order = listOf(UP, RIGHT, DOWN, LEFT)
        val idx = order.indexOf(this)
        val s = ((steps % 4) + 4) % 4
        return order[(idx + s) % 4]
    }
}

// endregion

// region Shapes & tile

enum class PipeShape(val baseConnections: Set<Side>) {
    STRAIGHT(setOf(Side.LEFT, Side.RIGHT)),
    // The EPS elbow art has the pipe ends at LEFT + UP at rotation 0 (curve in bottom-right).
    // We match the model to the image so rotation = visual rotation 1-to-1.
    ELBOW(setOf(Side.LEFT, Side.UP)),
    TEE(setOf(Side.LEFT, Side.RIGHT, Side.DOWN)),
    ENTRY(setOf(Side.DOWN)),
    EXIT(setOf(Side.UP)),
}

data class Tile(
    val shape: PipeShape,
    val rotation: Int,
    val locked: Boolean = false,
) {
    val connections: Set<Side> by lazy {
        shape.baseConnections.map { it.rotated(rotation) }.toSet()
    }
    fun rotated(): Tile = if (locked) this else copy(rotation = (rotation + 1) % 4)
    /** Counter-clockwise rotation — used as the long-press "flip" gesture. */
    fun rotatedCCW(): Tile = if (locked) this else copy(rotation = (rotation + 3) % 4)
}

data class Pos(val x: Int, val y: Int)

// endregion

// region Level data

data class LevelData(
    val id: Int,
    val name: String,
    val cols: Int,
    val rows: Int,
    val entryCol: Int,
    val exitCol: Int,
    val tiles: List<TileData>,
)

data class TileData(
    val x: Int,
    val y: Int,
    val shape: PipeShape,
    val rot: Int,
)

object LevelStore {
    @Volatile
    private var cache: List<LevelData>? = null

    fun all(context: Context): List<LevelData> {
        cache?.let { return it }
        val parsed = try {
            val raw = context.resources.openRawResource(R.raw.levels).use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            }
            val root = JSONObject(raw)
            val arr = root.getJSONArray("levels")
            (0 until arr.length()).map { i ->
                val l = arr.getJSONObject(i)
                val tilesArr = l.getJSONArray("tiles")
                val tiles = (0 until tilesArr.length()).map { j ->
                    val t = tilesArr.getJSONObject(j)
                    TileData(
                        x = t.getInt("x"),
                        y = t.getInt("y"),
                        shape = PipeShape.valueOf(t.getString("shape").uppercase()),
                        rot = t.getInt("rot"),
                    )
                }
                LevelData(
                    id = l.getInt("id"),
                    name = l.getString("name"),
                    cols = l.getInt("cols"),
                    rows = l.getInt("rows"),
                    entryCol = l.getInt("entry_col"),
                    exitCol = l.getInt("exit_col"),
                    tiles = tiles,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
        cache = parsed
        return parsed
    }
}

// endregion

// region Board

data class PipeBoard(
    val level: LevelData,
    val tiles: Map<Pos, Tile>,
) {
    val entryPos: Pos get() = Pos(level.entryCol, 0)
    val exitPos: Pos get() = Pos(level.exitCol, level.rows - 1)

    fun rotate(p: Pos): PipeBoard {
        val t = tiles[p] ?: return this
        if (t.locked) return this
        val newTiles = tiles.toMutableMap()
        newTiles[p] = t.rotated()
        return copy(tiles = newTiles)
    }

    /** Counter-clockwise rotate — long-press gesture. */
    fun flip(p: Pos): PipeBoard {
        val t = tiles[p] ?: return this
        if (t.locked) return this
        val newTiles = tiles.toMutableMap()
        newTiles[p] = t.rotatedCCW()
        return copy(tiles = newTiles)
    }

    fun reachableFromEntry(): Set<Pos> {
        val visited = mutableSetOf(entryPos)
        val queue: ArrayDeque<Pos> = ArrayDeque(); queue.addLast(entryPos)
        while (queue.isNotEmpty()) {
            val p = queue.removeFirst()
            val tile = tiles[p] ?: continue
            for (side in tile.connections) {
                val n = Pos(p.x + side.dx, p.y + side.dy)
                if (n in visited) continue
                val neighbor = tiles[n] ?: continue
                if (side.opposite() in neighbor.connections) {
                    visited.add(n)
                    queue.addLast(n)
                }
            }
        }
        return visited
    }

    val isSolved: Boolean get() = exitPos in reachableFromEntry()

    /**
     * Ordered path the marble follows: from entry, through the connected path,
     * to either the exit or the deepest reachable point (dead end).
     */
    fun ballPath(): List<Pos> {
        val parent = mutableMapOf<Pos, Pos>()
        val visited = mutableSetOf(entryPos)
        val queue: ArrayDeque<Pos> = ArrayDeque(); queue.addLast(entryPos)
        var deepest = entryPos
        var deepestY = 0
        var endNode: Pos? = null
        while (queue.isNotEmpty()) {
            val p = queue.removeFirst()
            if (p == exitPos) { endNode = p; break }
            if (p.y > deepestY) { deepestY = p.y; deepest = p }
            val tile = tiles[p] ?: continue
            for (side in tile.connections) {
                val n = Pos(p.x + side.dx, p.y + side.dy)
                if (n in visited) continue
                val neighbor = tiles[n] ?: continue
                if (side.opposite() in neighbor.connections) {
                    visited.add(n)
                    parent[n] = p
                    queue.addLast(n)
                }
            }
        }
        val path = mutableListOf<Pos>()
        var cur: Pos? = endNode ?: deepest
        while (cur != null) { path.add(cur); cur = parent[cur] }
        return path.reversed()
    }

    companion object {
        fun forLevel(level: LevelData): PipeBoard {
            val map = mutableMapOf<Pos, Tile>()
            map[Pos(level.entryCol, 0)] = Tile(PipeShape.ENTRY, 0, locked = true)
            map[Pos(level.exitCol, level.rows - 1)] = Tile(PipeShape.EXIT, 0, locked = true)
            for (td in level.tiles) {
                map[Pos(td.x, td.y)] = Tile(td.shape, td.rot, locked = false)
            }
            return PipeBoard(level, map)
        }
    }
}

// endregion