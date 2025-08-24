/*
 * MIT License
 *
 * Copyright (c) 2025 Dreamfire Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.dreamfirestudios.dreamconfig.Internal.Repo;

import com.dreamfirestudios.dreamconfig.Internal.Reflection.DreamConfigDeSerializer;
import com.dreamfirestudios.dreamconfig.Internal.Reflection.DreamConfigSerializer;
import com.dreamfirestudios.dreamconfig.Model.Interfaces.IDreamConfig;
import com.dreamfirestudios.dreamconfig.Object.DreamConfigObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

/// <summary>
/// Mongo-backed repository. Collection name is provided at construction; document _id = <c>cfg.documentID()</c>.
/// </summary>
/// <remarks>
/// This implementation bypasses YAML <see cref="DreamConfigObject"/> I/O and writes a BSON document with
/// structure: <c>{ _id: &lt;docId&gt;, body: { ...fields... } }</c>. It mirrors lifecycle hook usage of the YAML path.
/// </remarks>
/// <example>
/// <code>
/// MongoConfigRepository repo = new MongoConfigRepository(client, "game", "configs");
/// repo.save(cfg, null);
/// repo.load(cfg, null);
/// </code>
/// </example>
public final class MongoConfigRepository implements ConfigRepository {
    private final MongoCollection<Document> collection;

    /// <summary>
    /// Create a repository bound to a specific database and collection.
    /// </summary>
    /// <param name="client">Mongo client.</param>
    /// <param name="dbName">Database name.</param>
    /// <param name="collectionName">Collection name.</param>
    public MongoConfigRepository(MongoClient client, String dbName, String collectionName) {
        MongoDatabase db = client.getDatabase(dbName);
        this.collection = db.getCollection(collectionName);
    }

    /// <summary>
    /// Save config to MongoDB (upsert).
    /// </summary>
    /// <param name="cfg">Config to persist.</param>
    /// <param name="ignored">Unused (kept for interface compatibility).</param>
    public void save(IDreamConfig cfg, DreamConfigObject ignored) throws Exception {
        var tmp = new Document();
        var body = DreamConfigSerializer.writeObject(cfg.getClass(), cfg);
        tmp.put("_id", cfg.documentID());
        tmp.put("body", new Document(body));
        collection.replaceOne(new Document("_id", cfg.documentID()), tmp, new ReplaceOptions().upsert(true));
    }

    /// <summary>
    /// Load config from MongoDB by <c>_id</c> and populate <paramref name="cfg"/>.
    /// </summary>
    /// <param name="cfg">Target config.</param>
    /// <param name="ignored">Unused (kept for interface compatibility).</param>
    public void load(IDreamConfig cfg, DreamConfigObject ignored) throws Exception {
        Document doc = collection.find(Filters.eq("_id", cfg.documentID())).first();
        if (doc == null) return;
        Document body = doc.get("body", Document.class);
        if (body == null) return;

        // Convert BSON -> Java map tree expected by YAML-style deserializer
        HashMap<Object, Object> javaTree = fromBson(body);

        cfg.BeforeLoadConfig();
        DreamConfigDeSerializer.readObject(javaTree, cfg.getClass(), cfg);
        cfg.AfterLoadConfig();
    }

    /// <summary>Not used for Mongo; returns empty string.</summary>
    public String resolvePath(IDreamConfig cfg) { return ""; }

    /// <summary>Not used for Mongo; returns empty list.</summary>
    public List<File> listConfigFiles(JavaPlugin plugin, File baseDir) { return List.of(); }

    /// <summary>Maps a file to its document id (unused in Mongo); returns file name.</summary>
    public String fileToDocumentId(File f) { return f.getName(); }

    /// <summary>Render a primitive console representation (indent applied).</summary>
    public String renderConsole(Object value, int indent) { return " ".repeat(indent) + String.valueOf(value); }

    /// <summary>Return an indent string.</summary>
    public String indent(int spaces) { return " ".repeat(Math.max(0, spaces)); }

    /// <summary>Recursively convert BSON <see cref="Document"/> to Java map/list primitives.</summary>
    private static HashMap<Object, Object> fromBson(Document document) {
        HashMap<Object, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> e : document.entrySet()) out.put(e.getKey(), fromBsonValue(e.getValue()));
        return out;
    }

    /// <summary>Convert a single BSON value to plain Java types.</summary>
    private static Object fromBsonValue(Object v) {
        if (v instanceof Document d) return fromBson(d);
        if (v instanceof List<?> list) {
            ArrayList<Object> out = new ArrayList<>(list.size());
            for (Object item : list) out.add(fromBsonValue(item));
            return out;
        }
        return v;
    }
}