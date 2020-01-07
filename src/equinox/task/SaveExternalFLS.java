/*
 * Copyright 2018 Murat Artim (muratartim@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package equinox.task;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import equinox.Equinox;
import equinox.plugin.FileType;
import equinox.serverUtilities.Permission;
import equinox.utility.Utility;

/**
 * Class for save external FLS file task.
 *
 * @author Murat Artim
 * @date Mar 13, 2015
 * @time 2:04:57 PM
 */
public class SaveExternalFLS extends TemporaryFileCreatingTask<Void> {

	/** External stress sequence ID. */
	private final int sequenceID_;

	/** Output file. */
	private final File output_;

	/** Output file type. */
	private final FileType type_;

	/**
	 * Creates save external FLS task.
	 *
	 * @param sequenceID
	 *            External stress sequence ID.
	 * @param output
	 *            Output file.
	 * @param type
	 *            Output file type.
	 */
	public SaveExternalFLS(int sequenceID, File output, FileType type) {
		sequenceID_ = sequenceID;
		output_ = output;
		type_ = type;
	}

	@Override
	public String getTaskTitle() {
		return "Save external FLS file to '" + output_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.SAVE_FILE);

		// update progress info
		updateTitle("Saving FLS file to '" + output_.getName() + "'");

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// write to temporary file
				Path flsFile = writeTemporaryFile(statement);

				// FLS file format
				if (type_.equals(FileType.FLS)) {
					Files.copy(flsFile, output_.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
				else if (type_.equals(FileType.ZIP)) {
					Utility.zipFile(flsFile, output_, this);
				}
				else if (type_.equals(FileType.GZ)) {
					Utility.gzipFile(flsFile.toFile(), output_);
				}
			}
		}

		// return
		return null;
	}

	/**
	 * Writes temporary FLS file.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Path to temporary file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeTemporaryFile(Statement statement) throws Exception {

		// create path to temporary file
		Path flsFile = getWorkingDirectory().resolve("temporaryFLSFile.fls");

		// create output file writer
		try (BufferedWriter writer = Files.newBufferedWriter(flsFile, Charset.defaultCharset())) {

			// write header
			writer.write("# --------------------------------------------------------------");
			writer.newLine();
			writer.write("# date:   " + new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
			writer.newLine();
			writer.write("# time:     " + new SimpleDateFormat("hh:mm:ss").format(new Date()));
			writer.newLine();
			writer.write("# FLS file generated by Equinox Version " + Equinox.VERSION.toString());
			writer.newLine();
			writer.write("# --------------------------------------------------------------");
			writer.newLine();
			writer.write("# Flight number, identification: TF_UNIQUENUMBER_ID(flight+temperature+severity)");
			writer.newLine();

			// write sequence
			String sql = "select * from ext_fls_flights where sth_id = " + sequenceID_ + " order by flight_num asc";
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					String line = String.format("%6s", resultSet.getInt("flight_num"));
					String severity = resultSet.getString("severity");
					line += "  " + resultSet.getString("name") + " " + (severity.isEmpty() ? "AHAAHHHCHA" : severity);
					writer.write(line);
					writer.newLine();
				}
			}
		}

		// return temporary FLS file
		return flsFile;
	}
}