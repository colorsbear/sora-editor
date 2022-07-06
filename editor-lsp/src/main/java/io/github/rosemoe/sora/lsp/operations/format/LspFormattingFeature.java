/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.lsp.operations.format;

import android.util.Pair;

import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;

import java.util.concurrent.CompletableFuture;

import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.operations.Feature;
import io.github.rosemoe.sora.lsp.utils.LspUtils;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.TextRange;

public class LspFormattingFeature implements Feature<Pair<Content, TextRange>, Void> {

    private CompletableFuture<Object> future;
    private LspEditor editor;

    @Override
    public void install(LspEditor editor) {
        this.editor = editor;
    }

    @Override
    public void uninstall(LspEditor editor) {
        this.editor = null;
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }

    @Override
    public Void execute(Pair<Content, TextRange> data) {
        var manager = editor.getRequestManager();
        var formattingParams = new DocumentRangeFormattingParams();
        formattingParams.setOptions(editor.getOption(FormattingOptions.class));

        formattingParams.setTextDocument(LspUtils.createTextDocumentIdentifier(editor.getCurrentFileUri()));
        var textRange = data.second;
        formattingParams.setRange(new Range(
                new Position(textRange.getStart().line, textRange.getStart().column),
                new Position(textRange.getEnd().line, textRange.getEnd().column)
        ));

        var content = data.first;

        future = manager.rangeFormatting(formattingParams)
                .thenApply(list -> {

                    list.forEach(textEdit -> {
                        var range = textEdit.getRange();
                        var text = textEdit.getNewText();
                        content.replace(
                                range.getStart().getLine(), range.getStart().getCharacter(),
                                range.getEnd().getLine(), range.getEnd().getCharacter(),
                                text
                        );
                    });

                    return null;
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });

        //block
        future.join();
        return null;
    }


}
