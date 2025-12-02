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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReflectionConfiguration implements IReflectionConfiguration{

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static ReflectionConfiguration loadFromFile(File file) {
		log.atTrace().log("Entering loadFromFile with file: {}", file);
		List<IReflectionConfigurationEntry> entries;
		try {
			log.atDebug().log("Loading reflection configuration from file: {}", file);
			entries = objectMapper.readValue(file,
					objectMapper.getTypeFactory().constructCollectionType(List.class, ReflectConfigEntry.class));
			log.atInfo().log("Loaded {} reflection configuration entries from file", entries.size());
		} catch (IOException e) {
			log.atWarn().log("Failed to load reflection configuration from file, initializing empty list: {}", file);
			entries = new ArrayList<>();
		}
		ReflectionConfiguration config = new ReflectionConfiguration();
		config.setEntries(entries);
		log.atTrace().log("Exiting loadFromFile");
		return config;
	}

	private List<IReflectionConfigurationEntry> entries;

	public List<IReflectionConfigurationEntry> getEntries() {
		return entries;
	}

	@Override
	public void setEntries(List<IReflectionConfigurationEntry> entries) {
		log.atTrace().log("Setting entries with {} items", entries.size());
		this.entries = entries;
	}

	@Override
	public void addEntry(IReflectionConfigurationEntry entry) {
		log.atTrace().log("Entering addEntry for: {}", entry.getName());
		removeEntry(entry);
		entries.add(entry);
		log.atDebug().log("Added reflection configuration entry: {}", entry.getName());
		log.atTrace().log("Exiting addEntry");
	}

	@Override
	public void removeEntry(IReflectionConfigurationEntry entry) {
		log.atTrace().log("Entering removeEntry for: {}", entry.getName());
		entries = entries.stream().filter(e -> !e.getName().equals(entry.getName())).collect(Collectors.toList());
		log.atDebug().log("Removed reflection configuration entry: {}", entry.getName());
		log.atTrace().log("Exiting removeEntry");
	}

	@Override
	public void updateEntry(IReflectionConfigurationEntry updatedEntry) {
		log.atTrace().log("Entering updateEntry for: {}", updatedEntry.getName());
		for (int i = 0; i < entries.size(); i++) {
			IReflectionConfigurationEntry entry = entries.get(i);
			if (entry.getName().equals(updatedEntry.getName())) {
				entries.set(i, updatedEntry);
				log.atDebug().log("Updated reflection configuration entry: {}", updatedEntry.getName());
				break;
			}
		}
		log.atTrace().log("Exiting updateEntry");
	}

	@Override
	public void saveToFile(File file) throws IOException {
		log.atTrace().log("Entering saveToFile with file: {}", file);
		log.atDebug().log("Saving {} reflection configuration entries to file: {}", entries.size(), file);
		objectMapper.writeValue(file, this.entries);
		log.atInfo().log("Reflection configuration saved successfully to file: {}", file);
		log.atTrace().log("Exiting saveToFile");
	}

	@Override
	public Optional<IReflectionConfigurationEntry> findEntryByType(Class<?> type) {
		log.atTrace().log("Entering findEntryByType for type: {}", type.getName());
		Optional<IReflectionConfigurationEntry> result = entries.stream().filter(entry -> entry.getName().equals(type.getName())).findFirst();
		log.atDebug().log("Found entry for type {}: {}", type.getName(), result.isPresent());
		log.atTrace().log("Exiting findEntryByType");
		return result;
	}
}
