package ch.njol.skript.environment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import ch.njol.skript.config.Config;
import ch.njol.skript.config.SectionNode;

/**
 * Source where a script could be loaded.
 */
@FunctionalInterface
public interface ScriptSource {
	
	public class Result {
		
		private final SectionNode root;
		private final boolean hasChanged;
		
		public Result(SectionNode root, boolean hasChanged) {
			this.root = root;
			this.hasChanged = hasChanged;
		}

		public SectionNode getRoot() {
			return root;
		}
		
		public boolean isHasChanged() {
			return hasChanged;
		}
		
	}
	
	Result get() throws IOException;
	
	public static ScriptSource constant(SectionNode root) {
		return () -> new Result(root, false);
	}
	
	public static ScriptSource wrap(Supplier<SectionNode> supplier) {
		return new ScriptSource() {
			private AtomicReference<SectionNode> previous = new AtomicReference<>();
			
			@Override
			public Result get() throws IOException {
				SectionNode current = supplier.get();
				if (current == null) {
					throw new IllegalStateException("supplier is not providing sources");
				}
				SectionNode oldValue = previous.getAndSet(current);
				if (current != oldValue) { // SectionNode doesn't implement equals
					return new Result(current, true); // Changed
				} else {
					return new Result(current, false); // Not changed
				}
			}
			
		};
	}
	
	@SuppressWarnings("null")
	public static ScriptSource file(Path path, String name) {
		return new ScriptSource() {
			private AtomicReference<FileTime> prevModified = new AtomicReference<>();
			private volatile SectionNode previous;
			
			@Override
			public Result get() throws IOException {
				FileTime curModified = Files.getLastModifiedTime(path);
				FileTime oldValue = prevModified.getAndSet(curModified);
				if (curModified.equals(oldValue)) {
					return new Result(previous, false);
				} else {
					previous = new Config(Channels.newInputStream(FileChannel.open(path)),
							name, true, false, ":").getMainNode();
					return new Result(previous, true);
				}
			}
			
		};
	}
	
	public static ScriptSource text(String code, String name) {
		Result result;
		try {
			result = new Result(new Config(new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8)),
					name, true, false, ":").getMainNode(), false);
		} catch (IOException e) {
			throw new AssertionError("string in-memory IO failed");
		}
		return () -> result;
	}
}
