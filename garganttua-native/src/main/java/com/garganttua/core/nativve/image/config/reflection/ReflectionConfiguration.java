package com.garganttua.core.nativve.image.config.reflection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garganttua.core.nativve.IReflectionConfiguration;
import com.garganttua.core.nativve.IReflectionConfigurationEntry;

public class ReflectionConfiguration implements IReflectionConfiguration{

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static ReflectionConfiguration loadFromFile(File file) {
		List<IReflectionConfigurationEntry> entries;
		try {
			entries = objectMapper.readValue(file,
					objectMapper.getTypeFactory().constructCollectionType(List.class, ReflectConfigEntry.class));
		} catch (IOException e) {
			entries = new ArrayList<>();
		}
		ReflectionConfiguration config = new ReflectionConfiguration();
		config.setEntries(entries);
		return config;
	}

	private List<IReflectionConfigurationEntry> entries;

	public List<IReflectionConfigurationEntry> getEntries() {
		return entries;
	}

	@Override
	public void setEntries(List<IReflectionConfigurationEntry> entries) {
		this.entries = entries;
	}

	@Override
	public void addEntry(IReflectionConfigurationEntry entry) {
		removeEntry(entry);
		entries.add(entry);
	}

	@Override
	public void removeEntry(IReflectionConfigurationEntry entry) {
		entries = entries.stream().filter(e -> !e.getName().equals(entry.getName())).collect(Collectors.toList());
	}

	@Override
	public void updateEntry(IReflectionConfigurationEntry updatedEntry) {
		for (int i = 0; i < entries.size(); i++) {
			IReflectionConfigurationEntry entry = entries.get(i);
			if (entry.getName().equals(updatedEntry.getName())) {
				entries.set(i, updatedEntry);
				break;
			}
		}
	} 

	@Override
	public void saveToFile(File file) throws IOException {
		objectMapper.writeValue(file, this.entries);
	}

	@Override
	public Optional<IReflectionConfigurationEntry> findEntryByType(Class<?> type) {
		return entries.stream().filter(entry -> entry.getName().equals(type.getName())).findFirst();
	}
}
