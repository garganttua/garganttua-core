package com.garganttua.nativve.image.config.reflection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

public class ReflectConfig {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static ReflectConfig loadFromFile(File file) {
		List<ReflectConfigEntry> entries;
		try {
			entries = objectMapper.readValue(file,
					objectMapper.getTypeFactory().constructCollectionType(List.class, ReflectConfigEntry.class));
		} catch (IOException e) {
			entries = new ArrayList<ReflectConfigEntry>();
		}
		ReflectConfig config = new ReflectConfig();
		config.setEntries(entries);
		return config;
	}

	private List<ReflectConfigEntry> entries;

	public List<ReflectConfigEntry> getEntries() {
		return entries;
	}

	public void setEntries(List<ReflectConfigEntry> entries) {
		this.entries = entries;
	}

	public void addEntry(ReflectConfigEntry entry) {
		removeEntry(entry);
		entries.add(entry);
	}

	public void removeEntry(ReflectConfigEntry entry) {
		entries = entries.stream().filter(e -> !e.getName().equals(entry.getName())).collect(Collectors.toList());
	}

	public void updateEntry(ReflectConfigEntry updatedEntry) {
		for (int i = 0; i < entries.size(); i++) {
			ReflectConfigEntry entry = entries.get(i);
			if (entry.getName().equals(updatedEntry.getName())) {
				entries.set(i, updatedEntry);
				break;
			}
		}
	}

	public void saveToFile(File file) throws IOException {
		objectMapper.writeValue(file, this.entries);
	}

	public Optional<ReflectConfigEntry> findEntryByName(Class<?> clazz) {
		return entries.stream().filter(entry -> entry.getName().equals(clazz.getName())).findFirst();
	}
}
